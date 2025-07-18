import io
import sys
import os
import traceback
from typing import Any, Dict, List, Tuple, TYPE_CHECKING
import marimo as mo

from .security import SecurityValidator

if TYPE_CHECKING:
    from .session import NotebookSession

class MarimoCellExecutor:
    def __init__(self, session: 'NotebookSession'):
        self.session = session
        self.security_validator = SecurityValidator()

    def execute_cell(self, cell_id: str, code: str) -> Tuple[bool, List[Dict[str, Any]], str, Dict[str, Any]]:
        """Executes a cell and captures its output and errors."""
        
        is_valid, validation_error = self.security_validator.validate_code(code)
        if not is_valid:
            error_output = self._format_error(validation_error)
            return False, [error_output], str(validation_error), {}

        # Store current working directory to restore later
        original_cwd = os.getcwd()
        
        # Change to session's working directory if it exists
        if self.session.working_dir and os.path.exists(self.session.working_dir):
            os.chdir(self.session.working_dir)

        old_stdout = sys.stdout
        old_stderr = sys.stderr
        redirected_stdout = io.StringIO()
        redirected_stderr = io.StringIO()
        sys.stdout = redirected_stdout
        sys.stderr = redirected_stderr

        outputs = []
        error = ""
        success = False

        try:
            exec(code, self.session.globals)
            success = True
        except Exception as e:
            error = f"{type(e).__name__}: {e}"
            # Capture full traceback for detailed error logging
            tb = traceback.format_exc()
            outputs.append(self._format_error(tb))
        finally:
            # Restore original working directory
            os.chdir(original_cwd)
            
            sys.stdout = old_stdout
            sys.stderr = old_stderr

            stdout_val = redirected_stdout.getvalue()
            if stdout_val:
                outputs.append({"type": "TEXT", "content": stdout_val, "mime_type": "text/plain"})

            stderr_val = redirected_stderr.getvalue()
            if stderr_val and not error:
                # If there's something in stderr but no exception was caught, treat it as a warning/text output
                outputs.append({"type": "TEXT", "content": stderr_val, "mime_type": "text/plain"})
            elif stderr_val and error:
                 # If an exception was caught, the traceback is already in outputs.
                 # We can log the raw stderr_val if needed for debugging.
                 print(f"Stderr from failed execution: {stderr_val}")


        cell_state = self._get_cell_state()
        return success, outputs, error, cell_state

    def _get_cell_state(self) -> Dict[str, str]:
        """Gets a string representation of the current state of variables."""
        state = {}
        # Add marimo to the globals if not already present for context
        self.session.globals['mo'] = mo
        for name, value in self.session.globals.items():
            if not name.startswith('_') and name not in ['In', 'Out', 'exit', 'quit', 'get_ipython']:
                try:
                    state[name] = repr(value)
                except Exception:
                    state[name] = "Not Serializable"
        return state

    def _format_error(self, error_message: str) -> Dict[str, str]:
        """Formats an error message into the standard output structure."""
        return {
            'type': 'ERROR',
            'content': str(error_message),
            'mime_type': 'text/plain'
        }
