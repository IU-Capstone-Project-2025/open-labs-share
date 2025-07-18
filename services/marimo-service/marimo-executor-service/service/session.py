from datetime import datetime, timedelta
import uuid
import asyncio
import os
import tempfile
import shutil
from typing import Dict, Optional, Any, Tuple
import marimo as mo
from minio import Minio
from config import Config
from .executor import MarimoCellExecutor

class NotebookSession:
    def __init__(self, session_id: str, notebook_path: str, initial_code: str, component_id: Optional[str] = None):
        self.session_id = session_id
        self.notebook_path = notebook_path
        self.component_id = component_id
        self.globals = {}
        self.last_accessed = datetime.now()
        self.cell_outputs = {}
        self.working_dir = None
        self._setup_working_directory()
        self._initialize_namespace(initial_code)

    def _setup_working_directory(self):
        """Create a temporary working directory and download assets if component_id is provided."""
        self.working_dir = tempfile.mkdtemp(prefix=f"marimo_session_{self.session_id}_")
        
        if self.component_id:
            # Download assets from MinIO to working directory
            try:
                minio_client = Minio(
                    Config.MINIO_ENDPOINT,
                    access_key=Config.MINIO_ACCESS_KEY,
                    secret_key=Config.MINIO_SECRET_KEY,
                    secure=Config.MINIO_SECURE
                )
                
                # List all assets for this component
                asset_prefix = f"components/{self.component_id}/assets/"
                print(f"Looking for assets with prefix: {asset_prefix}")
                objects = minio_client.list_objects(Config.MINIO_BUCKET, prefix=asset_prefix, recursive=True)
                
                asset_count = 0
                for obj in objects:
                    asset_count += 1
                    print(f"Found asset: {obj.object_name}")
                    # Extract the filename from the full path
                    # Path format: components/{componentId}/assets/{assetType}/{filename}
                    relative_path = obj.object_name[len(asset_prefix):]
                    if '/' in relative_path:
                        # Skip the asset type directory and get just the filename
                        filename = relative_path.split('/')[-1]
                    else:
                        filename = relative_path
                    
                    if filename:  # Only process actual files, not directories
                        local_file_path = os.path.join(self.working_dir, filename)
                        print(f"Downloading {obj.object_name} to {local_file_path}")
                        minio_client.fget_object(Config.MINIO_BUCKET, obj.object_name, local_file_path)
                        print(f"Successfully downloaded: {filename}")
                
                print(f"Downloaded {asset_count} assets to working directory: {self.working_dir}")
                if asset_count == 0:
                    print(f"No assets found for component {self.component_id}")
                    # Let's also try the old path format in case assets were uploaded before the fix
                    old_asset_prefix = f"marimo/components/{self.component_id}/assets/"
                    print(f"Checking old path format with prefix: {old_asset_prefix}")
                    old_objects = minio_client.list_objects(Config.MINIO_BUCKET, prefix=old_asset_prefix, recursive=True)
                    old_count = 0
                    for obj in old_objects:
                        old_count += 1
                        print(f"Found old format asset: {obj.object_name}")
                        # Extract the filename from the full path
                        relative_path = obj.object_name[len(old_asset_prefix):]
                        if '/' in relative_path:
                            filename = relative_path.split('/')[-1]
                        else:
                            filename = relative_path
                        
                        if filename:  # Only download if we have a valid filename
                            local_file_path = os.path.join(self.working_dir, filename)
                            print(f"Downloading old format {obj.object_name} to {local_file_path}")
                            minio_client.fget_object(Config.MINIO_BUCKET, obj.object_name, local_file_path)
                            print(f"Successfully downloaded from old path: {filename}")
                    print(f"Downloaded {old_count} assets from old path format")
                        
            except Exception as e:
                print(f"Warning: Failed to download assets for component {self.component_id}: {e}")

    def _initialize_namespace(self, initial_code: str):
        """Initialize the global namespace and execute initial code."""
        self.globals.update({
            'mo': mo,
            'marimo': mo,
        })
        # Execute the initial code to populate the session
        if initial_code:
            executor = MarimoCellExecutor(self)
            executor.execute_cell('initialization', initial_code)

    def update_last_accessed(self):
        self.last_accessed = datetime.now()

    def is_expired(self, timeout_minutes: int) -> bool:
        return (datetime.now() - self.last_accessed) > timedelta(minutes=timeout_minutes)

    def cleanup(self):
        """Clean up the working directory."""
        if self.working_dir and os.path.exists(self.working_dir):
            try:
                shutil.rmtree(self.working_dir)
                print(f"Cleaned up working directory: {self.working_dir}")
            except Exception as e:
                print(f"Warning: Failed to clean up working directory {self.working_dir}: {e}")

    def get_state(self) -> dict:
        """Get session state"""
        return {
            'globals': {k: str(v) for k, v in self.globals.items()},
            'last_accessed': self.last_accessed.isoformat(),
            'cell_outputs': self.cell_outputs,
            'working_dir': self.working_dir
        }

class SessionManager:
    def __init__(self):
        self.sessions: Dict[str, NotebookSession] = {}
        self.max_sessions = Config.MAX_SESSIONS
        self.timeout_minutes = Config.SESSION_TIMEOUT_MINUTES
        self.minio_client = Minio(
            Config.MINIO_ENDPOINT,
            access_key=Config.MINIO_ACCESS_KEY,
            secret_key=Config.MINIO_SECRET_KEY,
            secure=Config.MINIO_SECURE
        )

    def create_session(self, session_id: str, notebook_path: str, component_id: Optional[str] = None) -> Tuple[str, NotebookSession]:
        """Create a new notebook session with a specific ID."""
        # Clean expired sessions first
        self._cleanup_expired_sessions()

        if session_id in self.sessions:
            # This could happen in a race condition, or if the client retries.
            # We can either return the existing session or raise an error.
            # For now, let's treat it as an error to be explicit.
            raise RuntimeError(f"Session with ID {session_id} already exists.")

        if len(self.sessions) >= self.max_sessions:
            raise RuntimeError("Maximum number of sessions reached")

        # Verify notebook exists in MinIO
        if not self._verify_notebook_exists(notebook_path):
            raise FileNotFoundError(f"Notebook {notebook_path} not found in MinIO")

        # Get the notebook content from MinIO
        initial_code = self.get_notebook_content(notebook_path)
        
        session = NotebookSession(session_id, notebook_path, initial_code, component_id)
        self.sessions[session_id] = session
        return session_id, session

    def get_session(self, session_id: str) -> Optional[NotebookSession]:
        """Get an existing session"""
        session = self.sessions.get(session_id)
        if session:
            if session.is_expired(self.timeout_minutes):
                self.end_session(session_id)
                return None
            session.update_last_accessed()
        return session

    def end_session(self, session_id: str):
        """End a session"""
        if session_id in self.sessions:
            # Clean up the session's working directory
            self.sessions[session_id].cleanup()
            del self.sessions[session_id]

    def _cleanup_expired_sessions(self):
        """Clean up expired sessions"""
        expired = [
            sid for sid, session in self.sessions.items()
            if session.is_expired(self.timeout_minutes)
        ]
        for sid in expired:
            self.end_session(sid)

    def _verify_notebook_exists(self, notebook_path: str) -> bool:
        """Verify notebook exists in MinIO - tries both component.py and notebook.py for backward compatibility"""
        # Remove leading slash for MinIO
        clean_path = notebook_path.lstrip('/')
        
        try:
            # Try the provided path first
            self.minio_client.stat_object(Config.MINIO_BUCKET, clean_path)
            return True
        except Exception:
            pass

        alt_path = clean_path.replace('component.py', 'notebook.py')
        try:
            self.minio_client.stat_object(Config.MINIO_BUCKET, alt_path)
            return True
        except Exception:
            pass
                
        return False

    def get_notebook_content(self, notebook_path: str) -> str:
        """Get notebook content from MinIO - tries both component.py and notebook.py for backward compatibility"""
        # Remove leading slash for MinIO
        clean_path = notebook_path.lstrip('/')
        
        response = None
        try:
            # Try the provided path first
            response = self.minio_client.get_object(Config.MINIO_BUCKET, clean_path)
            content = response.read().decode('utf-8')
            return content
        except Exception:
            if response:
                response.close()
                response.release_conn()
                response = None

        # Try notebook.py instead
        alt_path = clean_path.replace('component.py', 'notebook.py')
        try:
            response = self.minio_client.get_object(Config.MINIO_BUCKET, alt_path)
            content = response.read().decode('utf-8')
            return content
        except Exception:
            pass
        finally:
            if response:
                response.close()
                response.release_conn()
                response = None
                
        raise FileNotFoundError(f"Notebook/component file not found at {notebook_path}")
        