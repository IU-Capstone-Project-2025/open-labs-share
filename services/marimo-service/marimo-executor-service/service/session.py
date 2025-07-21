from datetime import datetime, timedelta
import uuid
import asyncio
import os
import tempfile
import shutil
import ast
import sys
from typing import Dict, Optional, Any, Tuple, List, Set
import marimo as mo
from minio import Minio
from config import Config
from .logging_config import get_logger

# Import the executor here to avoid circular imports
from typing import TYPE_CHECKING
if TYPE_CHECKING:
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
        self.logger = get_logger("session")
        # Widget state management
        self.widgets = {}  # widget_id -> widget_object mapping
        self.widget_dependencies = {}  # component_id -> [widget_ids] it depends on
        # Cell-variable tracking for session management
        self.cell_variables = {}  # cell_id -> set of variable names defined by this cell
        self.cell_imports = {}  # cell_id -> set of imported module names by this cell
        self.cell_globals_snapshot = {}  # cell_id -> dict of globals before execution
        self.cell_widgets = {}  # cell_id -> set of widget_ids created by this cell
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
                self.logger.debug(f"Looking for assets with prefix: {asset_prefix}")
                objects = minio_client.list_objects(Config.MINIO_BUCKET, prefix=asset_prefix, recursive=True)
                
                asset_count = 0
                for obj in objects:
                    asset_count += 1
                    self.logger.debug(f"Found asset: {obj.object_name}")
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
                        self.logger.debug(f"Downloading {obj.object_name} to {local_file_path}")
                        minio_client.fget_object(Config.MINIO_BUCKET, obj.object_name, local_file_path)
                        self.logger.info(f"Successfully downloaded: {filename}")
                
                self.logger.info(f"Downloaded {asset_count} assets to working directory: {self.working_dir}")
                if asset_count == 0:
                    self.logger.debug(f"No assets found for component {self.component_id}")
                    # Let's also try the old path format in case assets were uploaded before the fix
                    old_asset_prefix = f"marimo/components/{self.component_id}/assets/"
                    self.logger.debug(f"Checking old path format with prefix: {old_asset_prefix}")
                    old_objects = minio_client.list_objects(Config.MINIO_BUCKET, prefix=old_asset_prefix, recursive=True)
                    old_count = 0
                    for obj in old_objects:
                        old_count += 1
                        self.logger.debug(f"Found old format asset: {obj.object_name}")
                        # Extract the filename from the full path
                        relative_path = obj.object_name[len(old_asset_prefix):]
                        if '/' in relative_path:
                            filename = relative_path.split('/')[-1]
                        else:
                            filename = relative_path
                        
                        if filename:  # Only download if we have a valid filename
                            local_file_path = os.path.join(self.working_dir, filename)
                            self.logger.debug(f"Downloading old format {obj.object_name} to {local_file_path}")
                            minio_client.fget_object(Config.MINIO_BUCKET, obj.object_name, local_file_path)
                            self.logger.info(f"Successfully downloaded from old path: {filename}")
                    self.logger.info(f"Downloaded {old_count} assets from old path format")
                        
            except Exception as e:
                self.logger.warning(f"Failed to download assets for component {self.component_id}: {e}")

    def _initialize_namespace(self, initial_code: str):
        """Initialize the global namespace and execute initial code."""
        self.globals.update({
            'mo': mo,
            'marimo': mo,
        })
        # Execute the initial code to populate the session
        if initial_code:
            self.logger.debug("Executing initial code for session initialization")
            from .executor import MarimoCellExecutor
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
                self.logger.info(f"Cleaned up working directory: {self.working_dir}")
            except Exception as e:
                self.logger.warning(f"Failed to clean up working directory {self.working_dir}: {e}")

    # Cell-variable tracking methods for session management
    
    # Protected variables that should never be cleaned up
    PROTECTED_VARIABLES = {
        'mo', 'marimo', '__builtins__', '__name__', '__doc__', 
        '__package__', '__loader__', '__spec__', '__file__'
    }

    def _is_protected_variable(self, var_name: str) -> bool:
        """Check if variable should be protected from cleanup"""
        return (var_name in self.PROTECTED_VARIABLES or 
                var_name.startswith('_') or
                var_name.startswith('__'))

    def _capture_pre_execution_state(self, cell_id: str) -> Dict[str, Any]:
        """Capture globals state before cell execution"""
        # Create a deep copy of current globals to avoid reference issues
        return {k: v for k, v in self.globals.items()}

    def _track_cell_variables(self, cell_id: str, pre_state: Dict[str, Any]) -> None:
        """Track which variables this cell defined/modified"""
        current_vars = set(self.globals.keys())
        pre_vars = set(pre_state.keys())
        
        # Variables added by this cell
        new_vars = current_vars - pre_vars
        
        # Variables modified by this cell (compare object identity for performance)
        modified_vars = set()
        for var in pre_vars & current_vars:
            # Use 'is not' for object identity comparison to detect reassignment
            if self.globals[var] is not pre_state[var]:
                modified_vars.add(var)
        
        # Only track non-protected variables
        tracked_vars = {var for var in (new_vars | modified_vars) 
                       if not self._is_protected_variable(var)}
        
        # Store tracking info
        if tracked_vars:  # Only store if there are variables to track
            self.cell_variables[cell_id] = tracked_vars
        elif cell_id in self.cell_variables:
            # Clear tracking if no variables to track
            del self.cell_variables[cell_id]
            
        # Store the pre-execution snapshot for this cell
        self.cell_globals_snapshot[cell_id] = pre_state

    def get_cell_variables(self, cell_id: str) -> set:
        """Get the set of variables defined by a specific cell"""
        return self.cell_variables.get(cell_id, set())

    def get_all_tracked_cells(self) -> List[str]:
        """Get list of all cells that have tracked variables"""
        return list(self.cell_variables.keys())
    
    def _track_cell_imports(self, cell_id: str, code: str) -> None:
        """Track modules imported by this cell using AST parsing"""
        try:
            # Parse the code to find import statements
            tree = ast.parse(code)
            imported_modules = set()
            
            for node in ast.walk(tree):
                if isinstance(node, ast.Import):
                    # Handle: import module, import module as alias
                    for alias in node.names:
                        module_name = alias.name.split('.')[0]  # Get top-level module
                        imported_modules.add(module_name)
                        
                elif isinstance(node, ast.ImportFrom):
                    # Handle: from module import name, from module import name as alias
                    if node.module:
                        module_name = node.module.split('.')[0]  # Get top-level module
                        imported_modules.add(module_name)
            
            # Only track non-protected modules
            filtered_imports = {mod for mod in imported_modules 
                               if not self._is_protected_module(mod)}
            
            if filtered_imports:
                self.cell_imports[cell_id] = filtered_imports
            elif cell_id in self.cell_imports:
                # Clear tracking if no imports to track
                del self.cell_imports[cell_id]
                
        except (SyntaxError, ValueError) as e:
            # If code can't be parsed, skip import tracking
            self.logger.warning(f"Could not parse imports for cell '{cell_id}': {e}")
    
    def _cleanup_cell_imports(self, cell_id: str) -> None:
        """Remove modules imported by this cell if not used by other cells"""
        if cell_id not in self.cell_imports:
            return  # No imports to clean up for this cell
        
        modules_to_remove = []
        for module_name in self.cell_imports[cell_id]:
            if module_name in sys.modules:
                # Only remove if not imported by other cells and not protected
                if (not self._is_module_used_by_other_cells(cell_id, module_name) and
                    not self._is_protected_module(module_name)):
                    modules_to_remove.append(module_name)
        
        # Remove the modules from sys.modules
        for module_name in modules_to_remove:
            try:
                del sys.modules[module_name]
                self.logger.debug(f"Cleaned up imported module '{module_name}' from cell '{cell_id}'")
            except KeyError:
                # Module was already removed, ignore
                pass
        
        # Clear tracking for this cell
        del self.cell_imports[cell_id]
    
    def _is_module_used_by_other_cells(self, current_cell_id: str, module_name: str) -> bool:
        """Check if module is imported by other cells"""
        for cell_id, modules in self.cell_imports.items():
            if cell_id != current_cell_id and module_name in modules:
                return True
        return False
    
    def _is_protected_module(self, module_name: str) -> bool:
        """Check if module should be protected from cleanup"""
        # Protected modules that should never be cleaned up
        protected_modules = {
            'marimo', 'mo',  # Marimo itself
            'builtins', '__builtin__',  # Built-in modules
            'sys', 'os', 'io',  # Core system modules
            'typing', 'collections',  # Core Python modules
            'datetime', 'time',  # Time modules
            'json', 'pickle',  # Serialization modules
            'tempfile', 'shutil',  # File system modules
            'uuid', 'asyncio',  # Utility modules
            'ast', 'inspect',  # Introspection modules
        }
        
        return (module_name in protected_modules or 
                module_name.startswith('_') or  # Private modules
                module_name in ['minio', 'config'])  # Project-specific modules
    
    def get_cell_imports(self, cell_id: str) -> Set[str]:
        """Get the set of modules imported by a specific cell"""
        return self.cell_imports.get(cell_id, set())
    
    def get_import_preview(self, cell_id: str) -> Dict[str, Any]:
        """Preview what imports would be cleaned up for a cell (for debugging/testing)"""
        if cell_id not in self.cell_imports:
            return {
                'will_clean': [],
                'will_keep': [],
                'reason_kept': [],
                'no_imports': True
            }
        
        will_clean = []
        will_keep = []
        reason_kept = []
        
        for module_name in self.cell_imports[cell_id]:
            if module_name in sys.modules:
                if self._is_protected_module(module_name):
                    will_keep.append(module_name)
                    reason_kept.append(f"{module_name}: protected module")
                elif self._is_module_used_by_other_cells(cell_id, module_name):
                    will_keep.append(module_name)
                    reason_kept.append(f"{module_name}: used by other cells")
                else:
                    will_clean.append(module_name)
            else:
                # Module not in sys.modules, nothing to clean
                pass
        
        return {
            'will_clean': will_clean,
            'will_keep': will_keep,
            'reason_kept': reason_kept,
            'no_imports': False
        }

    # Widget cleanup coordination methods
    
    def _track_cell_widgets(self, cell_id: str) -> None:
        """Track widgets created by this cell by analyzing global variables"""
        if cell_id not in self.cell_variables:
            return  # No variables to check for widgets
        
        cell_widget_ids = set()
        for var_name in self.cell_variables[cell_id]:
            if var_name in self.globals:
                var_value = self.globals[var_name]
                widget_id = self._extract_widget_id_from_variable(var_value)
                if widget_id and widget_id in self.widgets:
                    cell_widget_ids.add(widget_id)
        
        if cell_widget_ids:
            self.cell_widgets[cell_id] = cell_widget_ids
        elif cell_id in self.cell_widgets:
            # Clear tracking if no widgets found
            del self.cell_widgets[cell_id]
    
    def _extract_widget_id_from_variable(self, var_value: Any) -> Optional[str]:
        """Extract widget ID from a variable value if it's a marimo widget"""
        # Check if the variable is a marimo widget
        if hasattr(var_value, '_id') and hasattr(var_value, '_kind'):
            # This looks like a marimo widget
            return getattr(var_value, '_id', None)
        
        # Check for common marimo widget attributes
        if hasattr(var_value, 'id') and str(type(var_value)).find('marimo') != -1:
            return getattr(var_value, 'id', None)
        
        return None
    
    def _cleanup_cell_widgets(self, cell_id: str) -> None:
        """Remove widgets created by this cell if not used by other cells"""
        if cell_id not in self.cell_widgets:
            return  # No widgets to clean up for this cell
        
        widgets_to_remove = []
        for widget_id in self.cell_widgets[cell_id]:
            if widget_id in self.widgets:
                # Only remove if not used by other cells and not protected
                if (not self._is_widget_used_by_other_cells(cell_id, widget_id) and
                    not self._is_protected_widget(widget_id)):
                    widgets_to_remove.append(widget_id)
        
        # Remove the widgets
        for widget_id in widgets_to_remove:
            try:
                del self.widgets[widget_id]
                self.logger.debug(f"Cleaned up widget '{widget_id}' from cell '{cell_id}'")
                
                # Also remove from global variables if it exists as a variable
                for var_name in list(self.globals.keys()):
                    if var_name in self.globals:
                        var_value = self.globals[var_name]
                        if (hasattr(var_value, '_id') and 
                            getattr(var_value, '_id', None) == widget_id):
                            del self.globals[var_name]
                            self.logger.debug(f"Cleaned up widget variable '{var_name}' from globals")
                            break
                            
            except KeyError:
                # Widget was already removed, ignore
                pass
        
        # Clear tracking for this cell
        del self.cell_widgets[cell_id]
    
    def _is_widget_used_by_other_cells(self, current_cell_id: str, widget_id: str) -> bool:
        """Check if widget is created/used by other cells"""
        for cell_id, widget_ids in self.cell_widgets.items():
            if cell_id != current_cell_id and widget_id in widget_ids:
                return True
        return False
    
    def _is_protected_widget(self, widget_id: str) -> bool:
        """Check if widget should be protected from cleanup"""
        # Some widgets might be system-level or persistent
        # For now, we'll be conservative and not protect any specific widgets
        # In the future, this could check for specific widget types or IDs
        return False
    
    def get_cell_widgets(self, cell_id: str) -> Set[str]:
        """Get the set of widget IDs created by a specific cell"""
        return self.cell_widgets.get(cell_id, set())
    
    def get_widget_cleanup_preview(self, cell_id: str) -> Dict[str, Any]:
        """Preview what widgets would be cleaned up for a cell (for debugging/testing)"""
        if cell_id not in self.cell_widgets:
            return {
                'will_clean': [],
                'will_keep': [],
                'reason_kept': [],
                'no_widgets': True
            }
        
        will_clean = []
        will_keep = []
        reason_kept = []
        
        for widget_id in self.cell_widgets[cell_id]:
            if widget_id in self.widgets:
                if self._is_protected_widget(widget_id):
                    will_keep.append(widget_id)
                    reason_kept.append(f"{widget_id}: protected widget")
                elif self._is_widget_used_by_other_cells(cell_id, widget_id):
                    will_keep.append(widget_id)
                    reason_kept.append(f"{widget_id}: used by other cells")
                else:
                    will_clean.append(widget_id)
            else:
                # Widget not in registry, nothing to clean
                pass
        
        return {
            'will_clean': will_clean,
            'will_keep': will_keep,
            'reason_kept': reason_kept,
            'no_widgets': False
        }

    # Variable cleanup methods for session management
    
    def _cleanup_cell_variables(self, cell_id: str) -> None:
        """Remove variables that were previously defined by this cell"""
        if cell_id not in self.cell_variables:
            return  # No variables to clean up for this cell
        
        variables_to_remove = []
        for var_name in self.cell_variables[cell_id]:
            if var_name in self.globals:
                # Only remove if not defined by other cells and not protected
                if (not self._is_variable_used_by_other_cells(cell_id, var_name) and
                    not self._is_protected_variable(var_name)):
                    variables_to_remove.append(var_name)
        
        # Remove the variables from globals
        for var_name in variables_to_remove:
            try:
                del self.globals[var_name]
                self.logger.debug(f"Cleaned up variable '{var_name}' from cell '{cell_id}'")
            except KeyError:
                # Variable was already removed, ignore
                pass
        
        # Also cleanup imports from this cell
        self._cleanup_cell_imports(cell_id)
        
        # Also cleanup widgets from this cell
        self._cleanup_cell_widgets(cell_id)
        
        # Clear tracking for this cell
        del self.cell_variables[cell_id]
        if cell_id in self.cell_globals_snapshot:
            del self.cell_globals_snapshot[cell_id]

    def _cleanup_conflicting_initial_variables(self, cell_id: str, new_code: str) -> None:
        """Clean up variables from initialization that conflict with current cell's new variables"""
        if cell_id == 'initialization':
            return  # Don't clean up initialization from itself
        
        if 'initialization' not in self.cell_variables:
            return  # No initialization variables to conflict with
        
        # Parse the new code to find what variables will be defined
        try:
            tree = ast.parse(new_code)
            new_variables = set()
            
            for node in ast.walk(tree):
                if isinstance(node, ast.Assign):
                    for target in node.targets:
                        if isinstance(target, ast.Name):
                            new_variables.add(target.id)
                elif isinstance(node, ast.AugAssign):
                    if isinstance(node.target, ast.Name):
                        new_variables.add(node.target.id)
                elif isinstance(node, ast.AnnAssign):
                    if isinstance(node.target, ast.Name):
                        new_variables.add(node.target.id)
        except (SyntaxError, ValueError):
            # If we can't parse the code, skip cleanup - execution will handle the error
            return
        
        # Check which initialization variables conflict with new variables
        init_variables = self.cell_variables['initialization'].copy()  # Copy to avoid modification during iteration
        variables_to_remove = []
        
        for var_name in new_variables:
            if var_name in init_variables and var_name in self.globals:
                # This variable from initialization will be redefined, so clean it up
                if not self._is_protected_variable(var_name):
                    variables_to_remove.append(var_name)
        
        # Remove conflicting variables and update tracking
        for var_name in variables_to_remove:
            try:
                del self.globals[var_name]
                self.cell_variables['initialization'].discard(var_name)
                self.logger.debug(f"Cleaned up conflicting initial variable '{var_name}' for cell '{cell_id}'")
            except KeyError:
                pass
        
        # If initialization cell has no more variables, clean up its tracking
        if not self.cell_variables['initialization']:
            del self.cell_variables['initialization']
            if 'initialization' in self.cell_globals_snapshot:
                del self.cell_globals_snapshot['initialization']

    def _cleanup_conflicting_initial_imports(self, cell_id: str, new_code: str) -> None:
        """Clean up imports from initialization that conflict with current cell's new imports"""
        if cell_id == 'initialization':
            return  # Don't clean up initialization from itself
        
        if 'initialization' not in self.cell_imports:
            return  # No initialization imports to conflict with
        
        # Parse the new code to find what modules will be imported
        try:
            tree = ast.parse(new_code)
            new_imports = set()
            
            for node in ast.walk(tree):
                if isinstance(node, ast.Import):
                    for alias in node.names:
                        module_name = alias.name.split('.')[0]
                        new_imports.add(module_name)
                elif isinstance(node, ast.ImportFrom):
                    if node.module:
                        module_name = node.module.split('.')[0]
                        new_imports.add(module_name)
        except (SyntaxError, ValueError):
            # If we can't parse the code, skip cleanup
            return
        
        # Check which initialization imports conflict with new imports
        init_imports = self.cell_imports['initialization'].copy()
        imports_to_remove = []
        
        for module_name in new_imports:
            if module_name in init_imports:
                # This module from initialization will be reimported, so clean it up
                if not self._is_protected_module(module_name):
                    imports_to_remove.append(module_name)
        
        # Remove conflicting modules and update tracking
        for module_name in imports_to_remove:
            try:
                if module_name in sys.modules:
                    del sys.modules[module_name]
                self.cell_imports['initialization'].discard(module_name)
                self.logger.debug(f"Cleaned up conflicting initial import '{module_name}' for cell '{cell_id}'")
            except KeyError:
                pass
        
        # If initialization cell has no more imports, clean up its tracking
        if not self.cell_imports['initialization']:
            del self.cell_imports['initialization']
            del self.cell_globals_snapshot[cell_id]

    def _is_variable_used_by_other_cells(self, current_cell_id: str, var_name: str) -> bool:
        """Check if variable is defined by other cells OR used by other cells"""
        for cell_id, variables in self.cell_variables.items():
            if cell_id != current_cell_id and var_name in variables:
                return True
        
        # ENHANCED: Check if variable is used (referenced) by other cells
        # by analyzing dependencies between cells
        return self._is_variable_referenced_by_other_cells(current_cell_id, var_name)
    
    def _is_variable_referenced_by_other_cells(self, current_cell_id: str, var_name: str) -> bool:
        """Check if a variable is referenced (used) by other cells"""
        # For now, we'll use a simple heuristic: if removing this variable would 
        # cause other cells' variables to become invalid, then it's referenced
        
        # Get cells that were executed after this one and might depend on it
        other_cells = [cell_id for cell_id in self.cell_variables.keys() 
                      if cell_id != current_cell_id]
        
        for other_cell_id in other_cells:
            # Check if this other cell's variables would be orphaned
            # This is a simplified dependency check - in a full implementation,
            # we'd parse the AST to find actual variable references
            other_vars = self.cell_variables[other_cell_id]
            
            # If the other cell has variables and this variable exists in globals,
            # assume there might be a dependency. This is conservative but safe.
            if other_vars and var_name in self.globals:
                # Do a more sophisticated check: try to identify if removing this
                # variable would break the other cell's execution context
                if self._might_have_dependency(other_cell_id, var_name):
                    return True
        
        return False
    
    def _might_have_dependency(self, cell_id: str, var_name: str) -> bool:
        """Enhanced check if a cell might depend on a variable"""
        
        if cell_id not in self.cell_variables:
            return False
        
        # For import variables and module references, we need to be smarter
        # Check if this is an import alias (like 'pd' for pandas)
        if self._is_import_alias(var_name):
            # If the other cell explicitly defined this variable, then there's no dependency
            # (the other cell created its own version of the import)
            if var_name in self.cell_variables[cell_id]:
                return False
            
            # For import aliases, only preserve if the other cell also imported the same module
            return self._other_cell_imports_same_module(cell_id, var_name)
        
        # For regular variables, use the original conservative approach:
        # If the other cell explicitly defined this variable, then there's no dependency
        # (the other cell created its own version)
        if var_name in self.cell_variables[cell_id]:
            return False
        
        # FIXED: For regular variables that the other cell didn't define itself,
        # only assume dependency if we have evidence of actual usage.
        # The previous logic was too conservative - just having variables in a cell
        # doesn't mean it depends on ALL other variables.
        # 
        # For now, be less conservative - only preserve variables if there's
        # explicit evidence of cross-cell dependency (which we don't track yet).
        # This matches the behavior users expect.
        return False

    def _is_import_alias(self, var_name: str) -> bool:
        """Check if a variable name is likely an import alias"""
        # Simple heuristic: common import aliases
        common_aliases = {'pd', 'np', 'plt', 'sns', 'os', 'sys', 'json', 'dt'}
        if var_name in common_aliases:
            return True
        
        # Check if any cell has imports and this variable appeared in that cell
        for cell_id, imports in self.cell_imports.items():
            if imports and cell_id in self.cell_variables:
                cell_vars = self.cell_variables[cell_id]
                if var_name in cell_vars:
                    return True
        
        return False

    def _other_cell_imports_same_module(self, cell_id: str, var_name: str) -> bool:
        """Check if another cell imports the same module as this variable represents"""
        # This is a simplified check - in a full implementation we'd need
        # to track which import aliases map to which modules
        if cell_id in self.cell_imports:
            # If the other cell has any imports, be conservative
            return len(self.cell_imports[cell_id]) > 0
        return False

    def get_cleanup_preview(self, cell_id: str) -> Dict[str, Any]:
        """Preview what variables would be cleaned up for a cell (for debugging/testing)"""
        if cell_id not in self.cell_variables:
            return {
                'will_clean': [],
                'will_keep': [],
                'reason_kept': {}
            }
        
        will_clean = []
        will_keep = []
        reason_kept = {}
        
        for var_name in self.cell_variables[cell_id]:
            if var_name not in self.globals:
                continue  # Variable already removed
                
            if self._is_protected_variable(var_name):
                will_keep.append(var_name)
                reason_kept[var_name] = "protected_variable"
            elif self._is_variable_used_by_other_cells(cell_id, var_name):
                will_keep.append(var_name)
                reason_kept[var_name] = "used_by_other_cells"
            else:
                will_clean.append(var_name)
        
        return {
            'will_clean': will_clean,
            'will_keep': will_keep,
            'reason_kept': reason_kept
        }

    def force_cleanup_all_cell_variables(self, cell_id: str) -> None:
        """Force cleanup of all variables for a cell, ignoring cross-cell dependencies.
        
        WARNING: This should only be used in special cases like session reset.
        Normal cleanup should use _cleanup_cell_variables() which respects dependencies.
        """
        if cell_id not in self.cell_variables:
            return
        
        for var_name in self.cell_variables[cell_id]:
            if (var_name in self.globals and 
                not self._is_protected_variable(var_name)):
                try:
                    del self.globals[var_name]
                    self.logger.debug(f"Force cleaned variable '{var_name}' from cell '{cell_id}'")
                except KeyError:
                    pass
        
        # Clear tracking
        del self.cell_variables[cell_id]
        if cell_id in self.cell_globals_snapshot:
            del self.cell_globals_snapshot[cell_id]
    
    def optimize_session_performance(self) -> Dict[str, Any]:
        """Optimize session performance by cleaning up stale references and compacting data"""
        stats = {
            'before': self._get_session_stats(),
            'optimizations_applied': []
        }
        
        # 1. Remove stale snapshots for cells that no longer exist
        stale_snapshots = []
        for cell_id in list(self.cell_globals_snapshot.keys()):
            if cell_id not in self.cell_variables:
                stale_snapshots.append(cell_id)
                del self.cell_globals_snapshot[cell_id]
        
        if stale_snapshots:
            stats['optimizations_applied'].append(f"Removed {len(stale_snapshots)} stale snapshots")
        
        # 2. Remove empty tracking entries
        empty_cells = []
        for cell_id in list(self.cell_variables.keys()):
            if not self.cell_variables[cell_id]:
                empty_cells.append(cell_id)
                del self.cell_variables[cell_id]
        
        if empty_cells:
            stats['optimizations_applied'].append(f"Removed {len(empty_cells)} empty cell entries")
        
        # 3. Cleanup orphaned imports (imports with no corresponding cells)
        orphaned_imports = []
        for cell_id in list(self.cell_imports.keys()):
            if cell_id not in self.cell_variables:
                orphaned_imports.append(cell_id)
                del self.cell_imports[cell_id]
        
        if orphaned_imports:
            stats['optimizations_applied'].append(f"Removed {len(orphaned_imports)} orphaned import entries")
        
        # 4. Cleanup orphaned widgets
        orphaned_widgets = []
        for cell_id in list(self.cell_widgets.keys()):
            if cell_id not in self.cell_variables:
                orphaned_widgets.append(cell_id)
                del self.cell_widgets[cell_id]
        
        if orphaned_widgets:
            stats['optimizations_applied'].append(f"Removed {len(orphaned_widgets)} orphaned widget entries")
        
        stats['after'] = self._get_session_stats()
        return stats
    
    def _get_session_stats(self) -> Dict[str, Any]:
        """Get current session statistics for performance monitoring"""
        return {
            'tracked_cells': len(self.cell_variables),
            'total_tracked_variables': sum(len(vars) for vars in self.cell_variables.values()),
            'total_imports': sum(len(imports) for imports in self.cell_imports.values()),
            'total_widgets': len(self.widgets),
            'total_cell_widgets': sum(len(widgets) for widgets in self.cell_widgets.values()),
            'snapshots': len(self.cell_globals_snapshot),
            'globals_size': len(self.globals)
        }
    
    def is_session_memory_heavy(self) -> bool:
        """Check if session is consuming excessive memory"""
        stats = self._get_session_stats()
        
        # Define thresholds for what constitutes "heavy" memory usage
        return (stats['tracked_cells'] > 100 or
                stats['total_tracked_variables'] > 1000 or
                stats['globals_size'] > 2000 or
                len(self.cell_globals_snapshot) > 50)
    
    def get_performance_recommendations(self) -> List[str]:
        """Get recommendations for improving session performance"""
        recommendations = []
        stats = self._get_session_stats()
        
        if stats['tracked_cells'] > 50:
            recommendations.append("Consider breaking down large notebooks into smaller components")
        
        if stats['total_tracked_variables'] > 500:
            recommendations.append("Some cells may be creating too many variables - consider cleanup")
        
        if stats['globals_size'] > 1000:
            recommendations.append("Global namespace is large - consider periodic cleanup")
        
        if len(self.cell_globals_snapshot) > 30:
            recommendations.append("Many snapshots stored - run optimize_session_performance()")
        
        # Check for potential memory leaks
        orphaned_count = 0
        for cell_id in self.cell_globals_snapshot:
            if cell_id not in self.cell_variables:
                orphaned_count += 1
        
        if orphaned_count > 5:
            recommendations.append(f"Detected {orphaned_count} orphaned snapshots - run optimize_session_performance()")
        
        return recommendations
    
    def detect_circular_dependencies(self) -> Dict[str, List[str]]:
        """Detect potential circular dependencies between cells"""
        # This is a simplified implementation - in practice, would need AST analysis
        # to detect true dependencies through variable usage
        
        potential_circles = {}
        
        # Look for cases where cells might have mutual dependencies
        for cell_a in self.cell_variables:
            for cell_b in self.cell_variables:
                if cell_a != cell_b:
                    # Check if cell_a variables are used by cell_b and vice versa
                    a_vars = self.cell_variables[cell_a]
                    b_vars = self.cell_variables[cell_b]
                    
                    # Simple heuristic: if cells share variables, there might be a dependency
                    shared_vars = a_vars.intersection(b_vars)
                    if shared_vars:
                        if cell_a not in potential_circles:
                            potential_circles[cell_a] = []
                        potential_circles[cell_a].append(cell_b)
        
        return potential_circles
    
    def handle_cleanup_conflicts(self, cell_id: str) -> Dict[str, Any]:
        """Handle conflicts when cleaning up variables that might cause issues"""
        conflicts = {
            'variables_in_conflict': [],
            'imports_in_conflict': [],
            'widgets_in_conflict': [],
            'resolutions': []
        }
        
        if cell_id not in self.cell_variables:
            return conflicts
        
        # Check for variable conflicts
        for var_name in self.cell_variables[cell_id]:
            dependent_cells = []
            for other_cell_id, other_vars in self.cell_variables.items():
                if other_cell_id != cell_id and var_name in other_vars:
                    dependent_cells.append(other_cell_id)
            
            if dependent_cells:
                conflicts['variables_in_conflict'].append({
                    'variable': var_name,
                    'dependent_cells': dependent_cells
                })
                conflicts['resolutions'].append(
                    f"Variable '{var_name}' kept due to dependencies in cells: {dependent_cells}"
                )
        
        # Check for import conflicts
        if cell_id in self.cell_imports:
            for module_name in self.cell_imports[cell_id]:
                dependent_cells = []
                for other_cell_id, other_imports in self.cell_imports.items():
                    if other_cell_id != cell_id and module_name in other_imports:
                        dependent_cells.append(other_cell_id)
                
                if dependent_cells:
                    conflicts['imports_in_conflict'].append({
                        'module': module_name,
                        'dependent_cells': dependent_cells
                    })
                    conflicts['resolutions'].append(
                        f"Module '{module_name}' kept due to dependencies in cells: {dependent_cells}"
                    )
        
        # Check for widget conflicts
        if cell_id in self.cell_widgets:
            for widget_id in self.cell_widgets[cell_id]:
                dependent_cells = []
                for other_cell_id, other_widgets in self.cell_widgets.items():
                    if other_cell_id != cell_id and widget_id in other_widgets:
                        dependent_cells.append(other_cell_id)
                
                if dependent_cells:
                    conflicts['widgets_in_conflict'].append({
                        'widget': widget_id,
                        'dependent_cells': dependent_cells
                    })
                    conflicts['resolutions'].append(
                        f"Widget '{widget_id}' kept due to dependencies in cells: {dependent_cells}"
                    )
        
        return conflicts
    
    def safe_cleanup_with_rollback(self, cell_id: str) -> Dict[str, Any]:
        """Perform cleanup with ability to rollback if issues are detected"""
        # Create backup of current state
        backup = {
            'cell_variables': dict(self.cell_variables),
            'cell_imports': dict(self.cell_imports),
            'cell_widgets': dict(self.cell_widgets),
            'globals_backup': {k: v for k, v in self.globals.items()},
            'widgets_backup': dict(self.widgets)
        }
        
        cleanup_result = {
            'success': False,
            'variables_cleaned': [],
            'imports_cleaned': [],
            'widgets_cleaned': [],
            'errors': [],
            'rollback_performed': False
        }
        
        try:
            # Perform cleanup and track what was cleaned
            if cell_id in self.cell_variables:
                original_vars = list(self.cell_variables[cell_id])
                original_imports = list(self.cell_imports.get(cell_id, []))
                original_widgets = list(self.cell_widgets.get(cell_id, []))
                
                # Perform the cleanup
                self._cleanup_cell_variables(cell_id)
                
                # Record what was actually cleaned
                for var_name in original_vars:
                    if var_name not in self.globals:
                        cleanup_result['variables_cleaned'].append(var_name)
                
                # Note: imports and widgets are cleaned as part of _cleanup_cell_variables
                cleanup_result['imports_cleaned'] = original_imports
                cleanup_result['widgets_cleaned'] = original_widgets
                
                cleanup_result['success'] = True
                
        except Exception as e:
            # If cleanup failed, rollback
            cleanup_result['errors'].append(str(e))
            cleanup_result['rollback_performed'] = True
            
            # Restore state
            self.cell_variables = backup['cell_variables']
            self.cell_imports = backup['cell_imports']
            self.cell_widgets = backup['cell_widgets']
            self.widgets = backup['widgets_backup']
            
            # Restore globals (more careful restoration)
            for var_name, var_value in backup['globals_backup'].items():
                if var_name not in self.globals:
                    self.globals[var_name] = var_value
        
        return cleanup_result
    
    def validate_session_integrity(self) -> Dict[str, Any]:
        """Validate that the session state is consistent and identify any issues"""
        issues = {
            'orphaned_snapshots': [],
            'missing_snapshots': [],
            'invalid_references': [],
            'memory_leaks': [],
            'consistency_errors': []
        }
        
        # Check for orphaned snapshots
        for cell_id in self.cell_globals_snapshot:
            if cell_id not in self.cell_variables:
                issues['orphaned_snapshots'].append(cell_id)
        
        # Check for missing snapshots
        for cell_id in self.cell_variables:
            if cell_id not in self.cell_globals_snapshot:
                issues['missing_snapshots'].append(cell_id)
        
        # Check for invalid variable references
        for cell_id, variables in self.cell_variables.items():
            for var_name in variables:
                if var_name not in self.globals and not self._is_protected_variable(var_name):
                    issues['invalid_references'].append(f"Cell {cell_id} tracks variable '{var_name}' not in globals")
        
        # Check for potential memory leaks
        if len(self.cell_globals_snapshot) > len(self.cell_variables) * 2:
            issues['memory_leaks'].append("Excessive snapshots compared to active cells")
        
        # Check for consistency between different tracking systems
        for cell_id in self.cell_imports:
            if cell_id not in self.cell_variables:
                issues['consistency_errors'].append(f"Cell {cell_id} has imports but no variables tracked")
        
        for cell_id in self.cell_widgets:
            if cell_id not in self.cell_variables:
                issues['consistency_errors'].append(f"Cell {cell_id} has widgets but no variables tracked")
        
        return issues

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
        self.logger = get_logger("session_manager")
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
        