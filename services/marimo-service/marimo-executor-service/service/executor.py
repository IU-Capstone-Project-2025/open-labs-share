import io
import sys
import os
import traceback
import ast
import base64
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
            # Parse the code to identify if last statement is an expression
            code_result = self._execute_with_expression_handling(code)
            
            # Check if the expression result is a matplotlib figure
            is_matplotlib_figure = False
            if code_result is not None and hasattr(code_result, 'savefig'):
                is_matplotlib_figure = True
            
            # If we have a result from the last expression, format it
            if code_result is not None:
                expression_output = self._format_expression_result(code_result)
                outputs.append(expression_output)
            
            # Check for matplotlib figures that might have been created but not returned
            # Only capture if we didn't already capture a matplotlib figure as expression result
            if not is_matplotlib_figure:
                matplotlib_figures = self._capture_matplotlib_figures()
                for fig_output in matplotlib_figures:
                    outputs.append(fig_output)
            
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

            # Handle stdout output (from print statements)
            stdout_val = redirected_stdout.getvalue()
            if stdout_val:
                outputs.insert(0, {"type": "STDOUT", "content": stdout_val, "mime_type": "text/plain"})

            # Handle stderr output
            stderr_val = redirected_stderr.getvalue()
            if stderr_val and not error:
                # If there's something in stderr but no exception was caught, treat it as a warning/text output
                outputs.insert(0, {"type": "STDERR", "content": stderr_val, "mime_type": "text/plain"})
            elif stderr_val and error:
                 # If an exception was caught, the traceback is already in outputs.
                 # We can log the raw stderr_val if needed for debugging.
                 print(f"Stderr from failed execution: {stderr_val}")

        cell_state = self._get_cell_state()
        return success, outputs, error, cell_state

    def _execute_with_expression_handling(self, code: str) -> Any:
        """Execute code with special handling for last expression."""
        code = code.strip()
        if not code:
            return None
            
        try:
            # Parse the code to analyze the structure
            parsed = ast.parse(code)
            
            if not parsed.body:
                return None
            
            # Check if the last statement is an expression
            last_node = parsed.body[-1]
            
            if isinstance(last_node, ast.Expr):
                # The last statement is an expression
                # Split the code into statements and the last expression
                lines = code.split('\n')
                
                # Find the start line of the last expression
                last_expr_start = last_node.lineno - 1
                last_expr_end = last_node.end_lineno if hasattr(last_node, 'end_lineno') else len(lines)
                
                # Extract statements before the last expression
                statements_lines = lines[:last_expr_start]
                expression_lines = lines[last_expr_start:last_expr_end]
                
                statements_code = '\n'.join(statements_lines).strip()
                expression_code = '\n'.join(expression_lines).strip()
                
                # Execute the statements first
                if statements_code:
                    exec(statements_code, self.session.globals)
                
                # Evaluate the last expression and return its result
                if expression_code:
                    result = eval(expression_code, self.session.globals)
                    return result
                    
            else:
                # No expression at the end, just execute normally
                exec(code, self.session.globals)
                return None
                
        except SyntaxError:
            # If parsing fails, fall back to regular execution
            exec(code, self.session.globals)
            return None
            
        return None

    def _capture_matplotlib_figures(self) -> List[Dict[str, Any]]:
        """Capture any matplotlib figures that were created but not returned."""
        figures = []
        
        try:
            # Try to import matplotlib
            import matplotlib.pyplot as plt
            
            # Get all current figures
            fig_nums = plt.get_fignums()
            
            for fig_num in fig_nums:
                fig = plt.figure(fig_num)
                
                # Check if the figure has any content (axes with data)
                if fig.get_axes():
                    # Check if any axes have data plotted
                    has_data = False
                    for ax in fig.get_axes():
                        if ax.lines or ax.patches or ax.collections or ax.images:
                            has_data = True
                            break
                    
                    if has_data:
                        # Format the figure
                        fig_output = self._format_matplotlib_figure(fig)
                        if fig_output:
                            figures.append(fig_output)
                
                # Close the figure to prevent memory leaks
                plt.close(fig)
                
        except ImportError:
            # Matplotlib not available
            pass
        except Exception:
            # Any other error in matplotlib handling
            pass
            
        return figures

    def _format_expression_result(self, result: Any) -> Dict[str, Any]:
        """Format the result of an expression based on its type."""
        if result is None:
            return {
                'type': 'EXPRESSION_RESULT',
                'content': 'None',
                'mime_type': 'text/plain',
                'data_type': 'TEXT'
            }
            
        # Handle pandas DataFrames
        if hasattr(result, '_repr_html_'):
            try:
                html_repr = result._repr_html_()
                return {
                    'type': 'EXPRESSION_RESULT',
                    'content': html_repr,
                    'mime_type': 'text/html',
                    'data_type': 'HTML'
                }
            except Exception:
                pass
        
        # Handle pandas DataFrames with fallback
        try:
            import pandas as pd
            if isinstance(result, pd.DataFrame):
                try:
                    # Try HTML representation first
                    html_repr = result._repr_html_()
                    return {
                        'type': 'EXPRESSION_RESULT',
                        'content': html_repr,
                        'mime_type': 'text/html',
                        'data_type': 'HTML'
                    }
                except Exception:
                    # Fallback to string representation
                    return {
                        'type': 'EXPRESSION_RESULT',
                        'content': str(result),
                        'mime_type': 'text/plain',
                        'data_type': 'TEXT'
                    }
            elif isinstance(result, pd.Series):
                try:
                    # Handle pandas Series
                    return {
                        'type': 'EXPRESSION_RESULT',
                        'content': str(result),
                        'mime_type': 'text/plain',
                        'data_type': 'TEXT'
                    }
                except Exception:
                    pass
        except ImportError:
            # Pandas not available, continue with other handlers
            pass
        except Exception:
            # Any other pandas-related error
            pass
        
        # Handle matplotlib/plotly figures
        if hasattr(result, 'savefig'):
            try:
                figure_output = self._format_matplotlib_figure(result)
                if figure_output:
                    return figure_output
            except Exception:
                pass
        
        # Handle lists, dicts, and other structured data
        if isinstance(result, (list, dict, tuple, set)):
            try:
                # For structured data, provide a nice representation
                import json
                if isinstance(result, (list, dict)):
                    # Try to serialize as JSON for better formatting
                    json_str = json.dumps(result, indent=2, default=str)
                    return {
                        'type': 'EXPRESSION_RESULT',
                        'content': json_str,
                        'mime_type': 'application/json',
                        'data_type': 'JSON'
                    }
                else:
                    # For tuples, sets, etc., use repr
                    return {
                        'type': 'EXPRESSION_RESULT',
                        'content': repr(result),
                        'mime_type': 'text/plain',
                        'data_type': 'TEXT'
                    }
            except Exception:
                pass
        
        # Handle numpy arrays
        if hasattr(result, 'shape') and hasattr(result, 'dtype'):
            try:
                # This is likely a numpy array
                shape = getattr(result, 'shape')
                dtype = getattr(result, 'dtype')
                
                # For small arrays, show the full content
                if hasattr(result, 'size') and result.size <= 100:
                    array_info = f"Array shape: {shape}, dtype: {dtype}\n{repr(result)}"
                else:
                    # For large arrays, show summary
                    array_info = f"Array shape: {shape}, dtype: {dtype}\n{str(result)}"
                
                return {
                    'type': 'EXPRESSION_RESULT',
                    'content': array_info,
                    'mime_type': 'text/plain',
                    'data_type': 'TEXT'
                }
            except Exception:
                pass
        
        # Default case: use repr() for string representation
        try:
            repr_str = repr(result)
            return {
                'type': 'EXPRESSION_RESULT',
                'content': repr_str,
                'mime_type': 'text/plain',
                'data_type': 'TEXT'
            }
        except Exception:
            return {
                'type': 'EXPRESSION_RESULT',
                'content': "Object not representable",
                'mime_type': 'text/plain',
                'data_type': 'TEXT'
            }

    def _format_matplotlib_figure(self, figure) -> Dict[str, Any]:
        """Format a matplotlib figure as base64 encoded PNG."""
        try:
            # Try to import matplotlib
            import matplotlib.pyplot as plt
            from io import BytesIO
            
            # Save figure to bytes
            img_buffer = BytesIO()
            figure.savefig(img_buffer, format='png', bbox_inches='tight', dpi=100)
            img_buffer.seek(0)
            
            # Encode as base64
            img_base64 = base64.b64encode(img_buffer.read()).decode('utf-8')
            
            # Close the buffer
            img_buffer.close()
            
            return {
                'type': 'EXPRESSION_RESULT',
                'content': f"data:image/png;base64,{img_base64}",
                'mime_type': 'image/png',
                'data_type': 'IMAGE'
            }
        except ImportError:
            # Matplotlib not available
            return {
                'type': 'EXPRESSION_RESULT',
                'content': "Matplotlib not available for figure display",
                'mime_type': 'text/plain',
                'data_type': 'TEXT'
            }
        except Exception as e:
            # If matplotlib handling fails, return error info
            return {
                'type': 'EXPRESSION_RESULT',
                'content': f"Error displaying figure: {str(e)}",
                'mime_type': 'text/plain',
                'data_type': 'TEXT'
            }

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
