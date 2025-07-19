from datetime import datetime, timedelta
import uuid
import asyncio
import os
import tempfile
import shutil
from typing import Dict, Optional, Any, Tuple, List
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
        # Widget state management
        self.widgets = {}  # widget_id -> widget_object mapping
        self.widget_dependencies = {}  # component_id -> [widget_ids] it depends on
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

    def add_widget(self, widget_id: str, widget_object: Any) -> None:
        """Add a widget to the session registry"""
        is_new_widget = widget_id not in self.widgets
        
        self.widgets[widget_id] = {
            'object': widget_object,
            'type': self._get_widget_type(widget_object),
            'properties': self._extract_widget_properties(widget_object),
            'value': getattr(widget_object, 'value', None),
            'dependencies': [],  # List of widget IDs this widget depends on
            'dependents': []     # List of widget IDs that depend on this widget
        }
        
        # Only update message for truly new widgets, not updates
        if is_new_widget:
            # Widget created successfully - no additional logging needed
            pass
        else:
            # Widget updated successfully - no additional logging needed  
            pass

    def update_widget_value(self, widget_id: str, new_value: Any) -> None:
        """Update a widget's value and trigger dependency updates"""
        if widget_id in self.widgets:
            widget_obj = self.widgets[widget_id]['object']
            if hasattr(widget_obj, '_value'):
                widget_obj._value = new_value
            elif hasattr(widget_obj, 'value'):
                widget_obj.value = new_value
            self.widgets[widget_id]['value'] = new_value
            # Widget value updated successfully
            
            # Trigger dependent widgets for re-evaluation
            self._trigger_dependent_widgets(widget_id)

    def _trigger_dependent_widgets(self, widget_id: str) -> None:
        """Trigger re-evaluation of widgets that depend on this widget"""
        if widget_id in self.widgets:
            dependents = self.widgets[widget_id]['dependents']
            for dependent_id in dependents:
                # Widget dependency triggered successfully
                # Mark dependent widgets as needing re-evaluation
                if dependent_id in self.widgets:
                    self.widgets[dependent_id]['needs_update'] = True

    def add_widget_dependency(self, widget_id: str, depends_on_id: str) -> None:
        """Add a dependency relationship between widgets"""
        if widget_id in self.widgets and depends_on_id in self.widgets:
            # Add dependency
            if depends_on_id not in self.widgets[widget_id]['dependencies']:
                self.widgets[widget_id]['dependencies'].append(depends_on_id)
            
            # Add dependent
            if widget_id not in self.widgets[depends_on_id]['dependents']:
                self.widgets[depends_on_id]['dependents'].append(widget_id)
            
            # Dependency added successfully

    def get_widget_dependencies(self, widget_id: str) -> List[str]:
        """Get the list of widgets this widget depends on"""
        if widget_id in self.widgets:
            return self.widgets[widget_id]['dependencies']
        return []

    def get_widget_dependents(self, widget_id: str) -> List[str]:
        """Get the list of widgets that depend on this widget"""
        if widget_id in self.widgets:
            return self.widgets[widget_id]['dependents']
        return []

    def get_widget(self, widget_id: str) -> Optional[Dict]:
        """Get widget information"""
        return self.widgets.get(widget_id)

    def validate_widget_value(self, widget_id: str, value: Any) -> Tuple[bool, str]:
        """Validate a widget value against its constraints"""
        if widget_id not in self.widgets:
            return False, "Widget not found"
        
        widget_info = self.widgets[widget_id]
        widget_type = widget_info['type']
        properties = widget_info['properties']
        
        try:
            if widget_type == 'slider':
                if not isinstance(value, (int, float)):
                    return False, "Slider value must be numeric"
                if 'start' in properties and value < properties['start']:
                    return False, f"Value {value} is below minimum {properties['start']}"
                if 'stop' in properties and value > properties['stop']:
                    return False, f"Value {value} is above maximum {properties['stop']}"
                if 'step' in properties and properties['step'] > 0:
                    min_val = properties.get('start', 0)
                    if (value - min_val) % properties['step'] != 0:
                        return False, f"Value {value} is not a valid step increment"
            
            elif widget_type == 'text':
                if not isinstance(value, str):
                    return False, "Text value must be a string"
                if 'max_length' in properties and len(value) > properties['max_length']:
                    return False, f"Text length {len(value)} exceeds maximum {properties['max_length']}"
            
            elif widget_type in ['dropdown', 'select', 'radio']:
                if 'options' in properties:
                    valid_values = [opt['value'] if isinstance(opt, dict) else opt for opt in properties['options']]
                    if value not in valid_values:
                        return False, f"Value {value} is not in valid options"
            
            elif widget_type == 'multiselect':
                if not isinstance(value, list):
                    return False, "Multiselect value must be a list"
                if 'options' in properties:
                    valid_values = [opt['value'] if isinstance(opt, dict) else opt for opt in properties['options']]
                    for v in value:
                        if v not in valid_values:
                            return False, f"Value {v} is not in valid options"
            
            elif widget_type == 'checkbox':
                if not isinstance(value, bool):
                    return False, "Checkbox value must be boolean"
            
            return True, ""
            
        except Exception as e:
            return False, f"Validation error: {str(e)}"

    def auto_fix_widget_value(self, widget_id: str, value: Any) -> Any:
        """Auto-fix invalid widget values"""
        if widget_id not in self.widgets:
            return value
        
        widget_info = self.widgets[widget_id]
        widget_type = widget_info['type']
        properties = widget_info['properties']
        
        try:
            if widget_type == 'slider':
                # Ensure numeric
                if not isinstance(value, (int, float)):
                    try:
                        value = float(value)
                    except:
                        value = properties.get('start', 0)
                
                # Clamp to bounds
                if 'start' in properties:
                    value = max(value, properties['start'])
                if 'stop' in properties:
                    value = min(value, properties['stop'])
                
                # Snap to step
                if 'step' in properties and properties['step'] > 0:
                    min_val = properties.get('start', 0)
                    value = min_val + round((value - min_val) / properties['step']) * properties['step']
            
            elif widget_type == 'text':
                # Ensure string
                if not isinstance(value, str):
                    value = str(value)
                
                # Truncate if too long
                if 'max_length' in properties:
                    value = value[:properties['max_length']]
            
            elif widget_type in ['dropdown', 'select', 'radio']:
                # Ensure valid option
                if 'options' in properties:
                    valid_values = [opt['value'] if isinstance(opt, dict) else opt for opt in properties['options']]
                    if value not in valid_values and valid_values:
                        value = valid_values[0]
            
            elif widget_type == 'multiselect':
                # Ensure list and valid options
                if not isinstance(value, list):
                    value = []
                if 'options' in properties:
                    valid_values = [opt['value'] if isinstance(opt, dict) else opt for opt in properties['options']]
                    value = [v for v in value if v in valid_values]
            
            elif widget_type == 'checkbox':
                # Ensure boolean
                if not isinstance(value, bool):
                    value = bool(value)
            
        except Exception as e:
            # Auto-fix failed for widget - continue with original value
            pass
        
        return value

    def batch_update_widgets(self, widget_updates: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Batch update multiple widgets for performance"""
        results = {
            'success': [],
            'failed': [],
            'total': len(widget_updates)
        }
        
        for update in widget_updates:
            widget_id = update.get('widget_id')
            value = update.get('value')
            
            # Skip if widget_id or value is None
            if widget_id is None or value is None:
                results['failed'].append({
                    'widget_id': widget_id,
                    'error': 'Missing widget_id or value'
                })
                continue
            
            try:
                # Validate and auto-fix
                is_valid, error_message = self.validate_widget_value(widget_id, value)
                if not is_valid:
                    value = self.auto_fix_widget_value(widget_id, value)
                
                # Update widget
                self.update_widget_value(widget_id, value)
                results['success'].append(widget_id)
                
            except Exception as e:
                results['failed'].append({
                    'widget_id': widget_id,
                    'error': str(e)
                })
        
        return results

    def get_widget_analytics(self) -> Dict[str, Any]:
        """Get widget analytics and performance metrics"""
        analytics = {
            'total_widgets': len(self.widgets),
            'widget_types': {},
            'total_updates': 0,
            'performance_metrics': {}
        }
        
        for widget_id, widget_info in self.widgets.items():
            widget_type = widget_info['type']
            analytics['widget_types'][widget_type] = analytics['widget_types'].get(widget_type, 0) + 1
            
            # Count updates if tracked
            if 'update_count' in widget_info:
                analytics['total_updates'] += widget_info['update_count']
        
        return analytics

    def _get_widget_type(self, widget_obj: Any) -> str:
        """Extract widget type from marimo widget object"""
        if hasattr(widget_obj, '__class__'):
            class_name = widget_obj.__class__.__name__.lower()
            # Handle range_slider specifically before removing underscores
            if class_name == 'range_slider':
                return 'range_slider'
            # Remove common prefixes/suffixes for other widgets
            widget_type = class_name.replace('widget', '').replace('ui', '').replace('_', '')
            return widget_type
        return 'unknown'

    def _extract_widget_properties(self, widget_obj: Any) -> Dict[str, Any]:
        """Extract widget properties for frontend rendering"""
        properties = {}
        
        # Get widget type to determine property mapping
        widget_type = self._get_widget_type(widget_obj)
        
        # Common widget properties with type-specific mappings
        if hasattr(widget_obj, 'start'):
            if widget_type in ['range_slider', 'slider']:
                properties['min'] = widget_obj.start  # Map start to min for sliders
            else:
                properties['start'] = widget_obj.start
        if hasattr(widget_obj, 'stop'):
            if widget_type in ['range_slider', 'slider']:
                properties['max'] = widget_obj.stop   # Map stop to max for sliders
            else:
                properties['stop'] = widget_obj.stop
        if hasattr(widget_obj, 'step'):
            properties['step'] = widget_obj.step
        if hasattr(widget_obj, 'label'):
            properties['label'] = widget_obj.label
        if hasattr(widget_obj, 'disabled'):
            properties['disabled'] = widget_obj.disabled
        if hasattr(widget_obj, 'options'):
            # Convert marimo's dictionary format to array format for frontend
            options_dict = widget_obj.options
            if isinstance(options_dict, dict):
                # Convert dictionary to array of objects
                converted_options = [
                    {'label': label, 'value': value} 
                    for label, value in options_dict.items()
                ]
                properties['options'] = converted_options
            else:
                properties['options'] = options_dict
        if hasattr(widget_obj, 'placeholder'):
            properties['placeholder'] = widget_obj.placeholder
        if hasattr(widget_obj, 'show_value'):
            properties['show_value'] = widget_obj.show_value
        if hasattr(widget_obj, 'orientation'):
            properties['orientation'] = widget_obj.orientation
        
        # For marimo widgets, extract label from _args tuple
        if hasattr(widget_obj, '_args') and isinstance(widget_obj._args, tuple) and len(widget_obj._args) > 2:
            label = widget_obj._args[2]
            if label and isinstance(label, str) and label.strip():
                properties['label'] = label
        
        return properties

    def validate_widget_value(self, widget_id: str, value: Any) -> Tuple[bool, str]:
        """Validate a widget value against its constraints"""
        if widget_id not in self.widgets:
            return False, f"Widget {widget_id} not found"
        
        widget = self.widgets[widget_id]
        widget_type = widget['type']
        properties = widget['properties']
        
        try:
            # Type-specific validation
            if widget_type == 'slider':
                if not isinstance(value, (int, float)):
                    return False, "Slider value must be numeric"
                
                if 'start' in properties and value < properties['start']:
                    return False, f"Value {value} is below minimum {properties['start']}"
                
                if 'stop' in properties and value > properties['stop']:
                    return False, f"Value {value} is above maximum {properties['stop']}"
                
                if 'step' in properties:
                    step = properties['step']
                    start = properties.get('start', 0)
                    if (value - start) % step != 0:
                        return False, f"Value {value} is not a valid step increment"
            
            elif widget_type == 'text':
                if not isinstance(value, str):
                    return False, "Text value must be a string"
                
                if 'maxLength' in properties and len(value) > properties['maxLength']:
                    return False, f"Text length {len(value)} exceeds maximum {properties['maxLength']}"
            
            elif widget_type == 'number':
                if not isinstance(value, (int, float)):
                    return False, "Number value must be numeric"
                
                if 'min' in properties and value < properties['min']:
                    return False, f"Value {value} is below minimum {properties['min']}"
                
                if 'max' in properties and value > properties['max']:
                    return False, f"Value {value} is above maximum {properties['max']}"
            
            elif widget_type in ['dropdown', 'select', 'radio']:
                if 'options' in properties:
                    valid_values = [opt['value'] if isinstance(opt, dict) else opt for opt in properties['options']]
                    if value not in valid_values:
                        return False, f"Value {value} is not in valid options {valid_values}"
            
            elif widget_type == 'multiselect':
                if not isinstance(value, list):
                    return False, "Multiselect value must be a list"
                if 'options' in properties:
                    valid_values = [opt['value'] if isinstance(opt, dict) else opt for opt in properties['options']]
                    for v in value:
                        if v not in valid_values:
                            return False, f"Value {v} is not in valid options {valid_values}"
            
            elif widget_type == 'checkbox':
                if not isinstance(value, bool):
                    return False, "Checkbox value must be boolean"
            
            return True, "Valid"
            
        except Exception as e:
            return False, f"Validation error: {str(e)}"

    def get_widget_constraints(self, widget_id: str) -> Dict[str, Any]:
        """Get validation constraints for a widget"""
        if widget_id not in self.widgets:
            return {}
        
        widget = self.widgets[widget_id]
        widget_type = widget['type']
        properties = widget['properties']
        constraints = {'type': widget_type}
        
        if widget_type == 'slider':
            constraints.update({
                'min': properties.get('start'),
                'max': properties.get('stop'),
                'step': properties.get('step')
            })
        elif widget_type == 'text':
            constraints.update({
                'maxLength': properties.get('maxLength')
            })
        elif widget_type == 'number':
            constraints.update({
                'min': properties.get('min'),
                'max': properties.get('max'),
                'step': properties.get('step')
            })
        elif widget_type in ['dropdown', 'select', 'radio']:
            constraints.update({
                'options': properties.get('options', [])
            })
        elif widget_type == 'multiselect':
            constraints.update({
                'options': properties.get('options', []),
                'maxSelections': properties.get('maxSelections')
            })
        
        return constraints

    def auto_fix_widget_value(self, widget_id: str, value: Any) -> Any:
        """Automatically fix widget values to conform to constraints"""
        if widget_id not in self.widgets:
            return value
        
        widget = self.widgets[widget_id]
        widget_type = widget['type']
        properties = widget['properties']
        
        try:
            if widget_type == 'slider':
                # Ensure numeric
                if not isinstance(value, (int, float)):
                    try:
                        value = float(value)
                    except (ValueError, TypeError):
                        value = properties.get('start', 0)
                
                # Clamp to bounds
                if 'start' in properties:
                    value = max(value, properties['start'])
                if 'stop' in properties:
                    value = min(value, properties['stop'])
                
                # Snap to step
                if 'step' in properties:
                    step = properties['step']
                    start = properties.get('start', 0)
                    value = start + round((value - start) / step) * step
            
            elif widget_type == 'text':
                # Ensure string
                if not isinstance(value, str):
                    value = str(value)
                
                # Truncate if too long
                if 'maxLength' in properties:
                    value = value[:properties['maxLength']]
            
            elif widget_type == 'number':
                # Ensure numeric
                if not isinstance(value, (int, float)):
                    try:
                        value = float(value)
                    except (ValueError, TypeError):
                        value = properties.get('min', 0)
                
                # Clamp to bounds
                if 'min' in properties:
                    value = max(value, properties['min'])
                if 'max' in properties:
                    value = min(value, properties['max'])
            
            elif widget_type in ['dropdown', 'select', 'radio']:
                # Ensure valid option
                if 'options' in properties:
                    valid_values = [opt['value'] if isinstance(opt, dict) else opt for opt in properties['options']]
                    if value not in valid_values and valid_values:
                        value = valid_values[0]  # Default to first option
            
            elif widget_type == 'multiselect':
                # Ensure list and valid options
                if not isinstance(value, list):
                    value = []
                if 'options' in properties:
                    valid_values = [opt['value'] if isinstance(opt, dict) else opt for opt in properties['options']]
                    value = [v for v in value if v in valid_values]
            
            elif widget_type == 'checkbox':
                # Ensure boolean
                if not isinstance(value, bool):
                    value = bool(value)
            
        except Exception:
            # If auto-fix fails, return original value
            pass
        
        return value

    def get_state(self) -> dict:
        """Get session state"""
        return {
            'globals': {k: str(v) for k, v in self.globals.items()},
            'last_accessed': self.last_accessed.isoformat(),
            'cell_outputs': self.cell_outputs,
            'working_dir': self.working_dir,
            'widgets': {k: {
                'type': v['type'],
                'properties': v['properties'],
                'value': v['value']
            } for k, v in self.widgets.items()}
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
        