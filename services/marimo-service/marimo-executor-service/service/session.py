from datetime import datetime, timedelta
import uuid
import asyncio
from typing import Dict, Optional, Any, Tuple
import marimo as mo
from minio import Minio
from config import Config
from .executor import MarimoCellExecutor

class NotebookSession:
    def __init__(self, session_id: str, notebook_path: str, initial_code: str):
        self.session_id = session_id
        self.notebook_path = notebook_path
        self.globals = {}
        self.last_accessed = datetime.now()
        self.cell_outputs = {}
        self._initialize_namespace(initial_code)

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

    def get_state(self) -> dict:
        """Get session state"""
        return {
            'globals': {k: str(v) for k, v in self.globals.items()},
            'last_accessed': self.last_accessed.isoformat(),
            'cell_outputs': self.cell_outputs
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

    def create_session(self, session_id: str, notebook_path: str) -> Tuple[str, NotebookSession]:
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
        
        session = NotebookSession(session_id, notebook_path, initial_code)
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
        """Verify notebook exists in MinIO"""
        try:
            # Note: MinIO paths don't have a leading slash
            self.minio_client.stat_object(Config.MINIO_BUCKET, notebook_path.lstrip('/'))
            return True
        except Exception:
            return False

    def get_notebook_content(self, notebook_path: str) -> str:
        """Get notebook content from MinIO"""
        response = None
        try:
            # Note: MinIO paths don't have a leading slash
            response = self.minio_client.get_object(Config.MINIO_BUCKET, notebook_path.lstrip('/'))
            content = response.read().decode('utf-8')
            return content
        finally:
            if response:
                response.close()
                response.release_conn()
        