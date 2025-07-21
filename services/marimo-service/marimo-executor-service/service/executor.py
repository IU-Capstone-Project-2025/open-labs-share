import io
import sys
import os
import traceback
import ast
import base64
import json
import hashlib
from typing import Any, Dict, List, Tuple, TYPE_CHECKING, Set, Optional
import marimo as mo

from .security import SecurityValidator
from .logging_config import get_logger

if TYPE_CHECKING:
    from .session import NotebookSession

class MarimoCellExecutor:
    def __init__(self, session: 'NotebookSession'):
        self.session = session
        self.security_validator = SecurityValidator()
        self.logger = get_logger("executor")

    def execute_cell(self, cell_id: str, code: str) -> Tuple[bool, List[Dict[str, Any]], str, Dict[str, Any]]:
        """Executes a cell and captures its output and errors."""
        
        self.logger.debug(f"Starting execution for cell '{cell_id}'")
        
        is_valid, validation_error = self.security_validator.validate_code(code)
        if not is_valid:
            self.logger.warning(f"Security validation failed for cell '{cell_id}': {validation_error}")
            error_output = self._format_error(validation_error)
            return False, [error_output], str(validation_error), {}

        # Clean up variables and track state
        cleanup_error = None
        pre_execution_state = None
        
        try:
            # Clean up variables from previous execution of this cell
            self.logger.debug(f"Cleaning up variables for cell '{cell_id}' before execution")
            self.session._cleanup_cell_variables(cell_id)
            
            # Clean up conflicting variables from initial code
            self.logger.debug(f"Cleaning up conflicting initial variables for cell '{cell_id}'")
            self.session._cleanup_conflicting_initial_variables(cell_id, code)
            
            # Clean up conflicting imports from initial code
            self.logger.debug(f"Cleaning up conflicting initial imports for cell '{cell_id}'")
            self.session._cleanup_conflicting_initial_imports(cell_id, code)
            
            # Capture pre-execution state for tracking
            pre_execution_state = self.session._capture_pre_execution_state(cell_id)
            
        except Exception as cleanup_ex:
            # If cleanup fails, log but continue with execution
            cleanup_error = f"Cleanup failed: {str(cleanup_ex)}"
            self.logger.warning(f"Variable cleanup warning for cell '{cell_id}': {cleanup_error}")
            # Still capture pre-state even if cleanup failed
            try:
                pre_execution_state = self.session._capture_pre_execution_state(cell_id)
            except Exception:
                pre_execution_state = {}

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
        processed_widgets = set()  # Track already processed widgets to prevent duplicates

        try:
            # Parse the code to identify if last statement is an expression
            code_result = self._execute_with_expression_handling(code)
            
            # Check if the expression result is a matplotlib figure
            is_matplotlib_figure = False
            if code_result is not None and hasattr(code_result, 'savefig'):
                is_matplotlib_figure = True
            
            # Check if the last expression result is a widget
            last_expression_is_widget = False
            if code_result is not None and self._is_marimo_widget(code_result):
                last_expression_is_widget = True
            
            # If we have a result from the last expression, format it
            if code_result is not None:
                expression_output = self._format_expression_result(code_result, processed_widgets)
                outputs.append(expression_output)
            
            # AST widget detection - only for widgets not covered by expression result
            # Skip AST detection if the last expression was already a widget
            if not last_expression_is_widget:
                widget_results = self._detect_widgets_in_code(code, processed_widgets)
                for widget_result in widget_results:
                    outputs.append(widget_result)
            
            # Check for matplotlib figures that might have been created but not returned
            # Only capture if we didn't already capture a matplotlib figure as expression result
            if not is_matplotlib_figure:
                matplotlib_figures = self._capture_matplotlib_figures()
                for fig_output in matplotlib_figures:
                    outputs.append(fig_output)
            
            # Track variables after successful execution
            try:
                self.logger.debug(f"Tracking variables for cell '{cell_id}' after successful execution")
                self.session._track_cell_variables(cell_id, pre_execution_state or {})
            except Exception as tracking_ex:
                # If tracking fails, log but don't fail the execution
                tracking_error = f"Variable tracking failed: {str(tracking_ex)}"
                self.logger.warning(f"Variable tracking warning for cell '{cell_id}': {tracking_error}")
                # Add warning to outputs but don't mark execution as failed
                outputs.append({
                    "type": "WARNING", 
                    "content": f"Variable tracking warning: {tracking_error}", 
                    "mime_type": "text/plain"
                })
            
            # Track imports after successful execution
            try:
                self.logger.debug(f"Tracking imports for cell '{cell_id}' after successful execution")
                self.session._track_cell_imports(cell_id, code)
            except Exception as import_tracking_ex:
                # If import tracking fails, log but don't fail the execution
                import_tracking_error = f"Import tracking failed: {str(import_tracking_ex)}"
                self.logger.warning(f"Import tracking warning for cell '{cell_id}': {import_tracking_error}")
                # Add warning to outputs but don't mark execution as failed
                outputs.append({
                    "type": "WARNING", 
                    "content": f"Import tracking warning: {import_tracking_error}", 
                    "mime_type": "text/plain"
                })
            
            # Track widgets after successful execution
            try:
                self.logger.debug(f"Tracking widgets for cell '{cell_id}' after successful execution")
                self.session._track_cell_widgets(cell_id)
            except Exception as widget_tracking_ex:
                # If widget tracking fails, log but don't fail the execution
                widget_tracking_error = f"Widget tracking failed: {str(widget_tracking_ex)}"
                self.logger.warning(f"Widget tracking warning for cell '{cell_id}': {widget_tracking_error}")
                # Add warning to outputs but don't mark execution as failed
                outputs.append({
                    "type": "WARNING", 
                    "content": f"Widget tracking warning: {widget_tracking_error}", 
                    "mime_type": "text/plain"
                })
            
            success = True
        except Exception as e:
            error = f"{type(e).__name__}: {e}"
            self.logger.error(f"Cell execution failed for '{cell_id}': {error}")
            # Capture full traceback for detailed error logging
            tb = traceback.format_exc()
            outputs.append(self._format_error(tb))
            
            # Track variables even after failed execution (for partial state)
            try:
                self.logger.debug(f"Tracking variables for cell '{cell_id}' after failed execution (partial state)")
                # Use empty pre-state if tracking failed during setup
                self.session._track_cell_variables(cell_id, pre_execution_state or {})
            except Exception as tracking_ex:
                tracking_error = f"Variable tracking failed after execution error: {str(tracking_ex)}"
                self.logger.warning(f"Variable tracking warning for cell '{cell_id}': {tracking_error}")
                # Don't add to outputs since we already have an execution error
            
            # Track imports even after failed execution (for partial state)
            try:
                self.logger.debug(f"Tracking imports for cell '{cell_id}' after failed execution (partial state)")
                self.session._track_cell_imports(cell_id, code)
            except Exception as import_tracking_ex:
                import_tracking_error = f"Import tracking failed after execution error: {str(import_tracking_ex)}"
                self.logger.warning(f"Import tracking warning for cell '{cell_id}': {import_tracking_error}")
                # Don't add to outputs since we already have an execution error
            
            # Track widgets even after failed execution (for partial state)
            try:
                self.logger.debug(f"Tracking widgets for cell '{cell_id}' after failed execution (partial state)")
                self.session._track_cell_widgets(cell_id)
            except Exception as widget_tracking_ex:
                widget_tracking_error = f"Widget tracking failed after execution error: {str(widget_tracking_ex)}"
                self.logger.warning(f"Widget tracking warning for cell '{cell_id}': {widget_tracking_error}")
                # Don't add to outputs since we already have an execution error
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
                 self.logger.warning(f"Stderr from failed execution: {stderr_val}")

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

    def _format_expression_result(self, result: Any, processed_widgets: Optional[Set] = None) -> Dict[str, Any]:
        """Format the result of an expression based on its type."""
        if processed_widgets is None:
            processed_widgets = set()
            
        if result is None:
            return {
                'type': 'EXPRESSION_RESULT',
                'content': 'None',
                'mime_type': 'text/plain',
                'data_type': 'TEXT'
            }
        
        # Check if result is a marimo widget
        if self._is_marimo_widget(result):
            # Create a unique identifier for this widget object
            widget_object_id = id(result)
            if widget_object_id in processed_widgets:
                # Widget already processed, return a simple representation instead
                return {
                    'type': 'EXPRESSION_RESULT',
                    'content': f'<marimo widget: {self._get_widget_type(result)}>',
                    'mime_type': 'text/plain',
                    'data_type': 'TEXT'
                }
            
            # Mark this widget as processed
            processed_widgets.add(widget_object_id)
            return self._format_widget_result(result)
            
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

    def _is_marimo_widget(self, obj: Any) -> bool:
        """Check if an object is a marimo widget"""
        if obj is None:
            return False
            
        # Check for direct marimo widget types
        if hasattr(obj, '__module__'):
            module_name = str(obj.__module__)
            if 'marimo' in module_name and ('ui' in module_name or 'UIElement' in str(type(obj))):
                return True
        
        # Check for marimo widget class patterns
        class_name = str(type(obj))
        if 'marimo' in class_name and any(widget_type in class_name.lower() for widget_type in 
                                         ['slider', 'button', 'text', 'checkbox', 'dropdown', 'select', 'radio', 'multiselect', 'number']):
            return True
        
        # Check if object has marimo widget methods/attributes
        widget_methods = ['_component', '_on_change', '_value', '_impl']
        if hasattr(obj, '_component') and any(hasattr(obj, method) for method in widget_methods):
            return True
        
        # Check for marimo UI elements by their behavior
        if hasattr(obj, 'value') and hasattr(obj, '_on_change') and hasattr(obj, '_component'):
            return True
            
        return False

    def _format_widget_result(self, result: Any) -> Dict[str, Any]:
        """Format marimo widget objects for output"""
        
        # Create a stable identifier for the widget based on its characteristics
        # instead of object identity which changes on each execution
        widget_characteristics = {
            'type': self._get_widget_type(result),
            'properties': self._extract_widget_properties(result),
            'value': self._get_widget_value(result)
        }
        
        # Create a hash from the widget characteristics for stable identification
        characteristics_str = json.dumps(widget_characteristics, sort_keys=True, default=str)
        widget_hash = hashlib.md5(characteristics_str.encode()).hexdigest()[:8]
        
        # Check if a widget with similar characteristics already exists
        existing_widget_id = None
        for existing_id, widget_info in self.session.widgets.items():
            existing_characteristics = {
                'type': widget_info['type'],
                'properties': widget_info['properties'],
                'value': widget_info['value']
            }
            existing_str = json.dumps(existing_characteristics, sort_keys=True, default=str)
            existing_hash = hashlib.md5(existing_str.encode()).hexdigest()[:8]
            
            if existing_hash == widget_hash:
                existing_widget_id = existing_id
                break
        
        # If widget with same characteristics already exists, return existing widget data
        if existing_widget_id:
            # Update the object reference to the new instance
            self.session.widgets[existing_widget_id]['object'] = result
            
            widget_data = {
                'id': existing_widget_id,
                'type': self.session.widgets[existing_widget_id]['type'],
                'value': self._get_widget_value(result),
                'properties': self.session.widgets[existing_widget_id]['properties']
            }
            
            return {
                'type': 'WIDGET',
                'content': json.dumps(widget_data),
                'mime_type': 'application/json',
                'data_type': 'WIDGET_DATA'
            }
        
        # Create new widget if not already registered
        widget_id = f"widget_{widget_hash}"  # Use hash for consistent ID
        
        # Detect widget type and properties
        widget_type = self._get_widget_type(result)
        
        # Extract widget properties
        properties = self._extract_widget_properties(result)
        
        # Get current value with fallback
        value = self._get_widget_value(result)
        
        # Store widget in session registry
        self.session.add_widget(widget_id, result)
        
        widget_data = {
            'id': widget_id,
            'type': widget_type,
            'value': value,
            'properties': properties
        }
        
        return {
            'type': 'WIDGET',
            'content': json.dumps(widget_data),
            'mime_type': 'application/json',
            'data_type': 'WIDGET_DATA'
        }

    def _get_widget_type(self, obj: Any) -> str:
        """Detect widget type with fallbacks"""
        # Check class name for type hints
        class_name = str(type(obj)).lower()
        
        # Direct class name mapping - check range_slider first before regular slider
        if 'range_slider' in class_name:
            return 'range_slider'
        elif 'slider' in class_name:
            return 'slider'
        elif 'button' in class_name:
            return 'button'
        elif 'text' in class_name:
            return 'text'
        elif 'checkbox' in class_name:
            return 'checkbox'
        elif 'radio' in class_name:
            return 'radio'
        elif 'multiselect' in class_name:
            return 'multiselect'
        elif 'dropdown' in class_name or 'select' in class_name:
            return 'dropdown'
        elif 'number' in class_name:
            return 'number'
        
        # Check for component type attribute
        if hasattr(obj, '_component'):
            component = obj._component
            if hasattr(component, 'component_type'):
                return str(component.component_type).lower()
        
        # Check for widget-specific attributes
        if hasattr(obj, 'min') and hasattr(obj, 'max'):
            return 'slider'
        elif hasattr(obj, 'options'):
            # Try to distinguish between different option-based widgets
            # Check class name more carefully
            class_name = str(type(obj)).lower()
            if 'radio' in class_name:
                return 'radio'
            elif 'multiselect' in class_name or 'multi_select' in class_name:
                return 'multiselect'
            elif 'dropdown' in class_name:
                return 'dropdown'
            else:
                # Default to dropdown for options-based widgets
                return 'dropdown'
        elif hasattr(obj, 'placeholder'):
            return 'text'
        
        # Fallback to session method
        return self.session._get_widget_type(obj)

    def _extract_widget_properties(self, obj: Any) -> Dict[str, Any]:
        """Extract widget properties"""
        properties = {}
        
        # Common properties
        if hasattr(obj, 'label'):
            properties['label'] = getattr(obj, 'label')
        
        # For marimo widgets, extract label from _args tuple
        if hasattr(obj, '_args') and isinstance(obj._args, tuple) and len(obj._args) > 2:
            label = obj._args[2]
            if label and isinstance(label, str) and label.strip():
                properties['label'] = label
        
        # Type-specific properties
        widget_type = self._get_widget_type(obj)
        
        if widget_type == 'range_slider':
            if hasattr(obj, 'start'):
                properties['min'] = getattr(obj, 'start')  # Map start to min for frontend compatibility
            if hasattr(obj, 'stop'):
                properties['max'] = getattr(obj, 'stop')   # Map stop to max for frontend compatibility
            if hasattr(obj, 'step'):
                properties['step'] = getattr(obj, 'step')
            # For range slider, also extract min/max from _args
            if hasattr(obj, '_args') and isinstance(obj._args, tuple) and len(obj._args) > 4:
                if isinstance(obj._args[4], dict):
                    config = obj._args[4]
                    if 'start' in config:
                        properties['min'] = config['start']
                    if 'stop' in config:
                        properties['max'] = config['stop']
                    if 'step' in config:
                        properties['step'] = config['step']
        elif widget_type == 'slider':
            # First try direct attributes
            if hasattr(obj, 'min'):
                properties['min'] = getattr(obj, 'min')
            if hasattr(obj, 'max'):
                properties['max'] = getattr(obj, 'max')
            if hasattr(obj, 'step'):
                properties['step'] = getattr(obj, 'step')
            
            # For marimo sliders, also extract from _args tuple
            # mo.ui.slider(start, stop, step=1, value=None, label="", ...)
            if hasattr(obj, '_args') and isinstance(obj._args, tuple):
                if len(obj._args) > 0 and obj._args[0] is not None:
                    properties['min'] = obj._args[0]  # start parameter
                if len(obj._args) > 1 and obj._args[1] is not None:
                    properties['max'] = obj._args[1]  # stop parameter
                if len(obj._args) > 2 and obj._args[2] is not None:
                    properties['step'] = obj._args[2]  # step parameter
            
            # Also try alternative attribute names that marimo might use
            if hasattr(obj, 'start'):
                properties['min'] = getattr(obj, 'start')
            if hasattr(obj, 'stop'):
                properties['max'] = getattr(obj, 'stop')
        
        elif widget_type == 'text':
            # First try direct attributes
            if hasattr(obj, 'placeholder'):
                properties['placeholder'] = getattr(obj, 'placeholder')
            if hasattr(obj, 'max_length'):
                properties['maxLength'] = getattr(obj, 'max_length')
            
            # For marimo text widgets, also extract from _args tuple
            # mo.ui.text(value="", placeholder="", label="", ...)
            if hasattr(obj, '_args') and isinstance(obj._args, tuple):
                if len(obj._args) > 1 and obj._args[1] is not None:
                    properties['placeholder'] = obj._args[1]  # placeholder parameter
                # Additional parameters might be in kwargs
                if len(obj._args) > 3 and isinstance(obj._args[3], dict):
                    kwargs = obj._args[3]
                    if 'placeholder' in kwargs:
                        properties['placeholder'] = kwargs['placeholder']
                    if 'max_length' in kwargs:
                        properties['maxLength'] = kwargs['max_length']
        
        elif widget_type in ['dropdown', 'select', 'radio', 'multiselect']:
            # First try direct attributes
            if hasattr(obj, 'options'):
                options = getattr(obj, 'options', [])
                if isinstance(options, (list, tuple)):
                    properties['options'] = [
                        {'value': opt, 'label': str(opt)} if not isinstance(opt, dict) else opt
                        for opt in options
                    ]
                elif isinstance(options, dict):
                    # Handle dictionary format: {label: value, ...}
                    properties['options'] = [
                        {'value': value, 'label': label}
                        for label, value in options.items()
                    ]
            
            # For marimo widgets, also extract options from _args tuple
            # mo.ui.dropdown(options, value=None, label="", ...)
            # mo.ui.radio(options, value=None, label="", ...)
            # mo.ui.multiselect(options, value=None, label="", ...)
            if hasattr(obj, '_args') and isinstance(obj._args, tuple):
                if len(obj._args) > 0 and obj._args[0] is not None:
                    options_arg = obj._args[0]
                    if isinstance(options_arg, (list, tuple)):
                        properties['options'] = [
                            {'value': opt, 'label': str(opt)} if not isinstance(opt, dict) else opt
                            for opt in options_arg
                        ]
                    elif isinstance(options_arg, dict):
                        # Handle dictionary format: {label: value, ...}
                        properties['options'] = [
                            {'value': value, 'label': label}
                            for label, value in options_arg.items()
                        ]
        
        elif widget_type == 'number':
            # First try direct attributes
            if hasattr(obj, 'min'):
                properties['min'] = getattr(obj, 'min')
            if hasattr(obj, 'max'):
                properties['max'] = getattr(obj, 'max')
            if hasattr(obj, 'step'):
                properties['step'] = getattr(obj, 'step')
            
            # For marimo number widgets, also extract from _args tuple
            # mo.ui.number(start=0, stop=100, step=1, value=None, label="", ...)
            if hasattr(obj, '_args') and isinstance(obj._args, tuple):
                if len(obj._args) > 0 and obj._args[0] is not None:
                    properties['min'] = obj._args[0]  # start parameter
                if len(obj._args) > 1 and obj._args[1] is not None:
                    properties['max'] = obj._args[1]  # stop parameter
                if len(obj._args) > 2 and obj._args[2] is not None:
                    properties['step'] = obj._args[2]  # step parameter
        
        elif widget_type == 'button':
            if hasattr(obj, 'kind'):
                properties['kind'] = getattr(obj, 'kind')
        
        # Fallback to session method for additional properties
        session_properties = self.session._extract_widget_properties(obj)
        properties.update(session_properties)
        
        return properties

    def _get_widget_value(self, obj: Any) -> Any:
        """Get widget value with fallbacks"""
        # Try direct value attribute
        if hasattr(obj, 'value'):
            return getattr(obj, 'value')
        
        # Try _value attribute
        if hasattr(obj, '_value'):
            return getattr(obj, '_value')
        
        # Try component value
        if hasattr(obj, '_component') and hasattr(obj._component, 'value'):
            return getattr(obj._component, 'value')
        
        # For marimo widgets, try to extract initial value from _args
        widget_type = self._get_widget_type(obj)
        if hasattr(obj, '_args') and isinstance(obj._args, tuple):
            if widget_type == 'slider':
                # mo.ui.slider(start, stop, step=1, value=None, ...)
                # Value is typically the 4th parameter or in kwargs
                if len(obj._args) > 3 and obj._args[3] is not None:
                    return obj._args[3]
                # If no explicit value, default to start value
                elif len(obj._args) > 0 and obj._args[0] is not None:
                    return obj._args[0]
            elif widget_type in ['dropdown', 'select', 'radio']:
                # mo.ui.dropdown(options, value=None, ...)
                # Value is typically the 2nd parameter
                if len(obj._args) > 1 and obj._args[1] is not None:
                    return obj._args[1]
            elif widget_type == 'multiselect':
                # mo.ui.multiselect(options, value=None, ...)
                # Value is typically the 2nd parameter and should be a list
                if len(obj._args) > 1 and obj._args[1] is not None:
                    value = obj._args[1]
                    return value if isinstance(value, list) else [value]
            elif widget_type == 'text':
                # mo.ui.text(value="", ...)
                # Value is typically the 1st parameter
                if len(obj._args) > 0 and obj._args[0] is not None:
                    return obj._args[0]
            elif widget_type == 'number':
                # mo.ui.number(start, stop, step=1, value=None, ...)
                # Value is typically the 4th parameter or start value
                if len(obj._args) > 3 and obj._args[3] is not None:
                    return obj._args[3]
                elif len(obj._args) > 0 and obj._args[0] is not None:
                    return obj._args[0]
        
        # Default values based on widget type
        if widget_type == 'range_slider':
            # For range slider, return default range
            return [0, 100]
        elif widget_type == 'slider':
            return 0
        elif widget_type == 'text':
            return ""
        elif widget_type == 'checkbox':
            return False
        elif widget_type in ['dropdown', 'select', 'radio']:
            return None
        elif widget_type == 'multiselect':
            return []
        elif widget_type == 'number':
            return 0
        
        return None

    def _detect_widgets_in_code(self, code: str, processed_widgets: Optional[Set] = None) -> List[Dict[str, Any]]:
        """AST-based widget detection for complex expressions"""
        if processed_widgets is None:
            processed_widgets = set()
            
        widgets = []
        
        try:
            # Parse the code into an AST
            tree = ast.parse(code)
            
            # Create a widget detector visitor
            detector = WidgetDetectorVisitor(self.session)
            detector.visit(tree)
            
            # Only show widgets from standalone expressions or function calls,
            # NOT from simple assignments (which would create duplicates)
            # Simple assignments like "checkbox = mo.ui.checkbox(...)" should not show output
            
            # Check function calls that might return widgets (standalone expressions)
            for call_info in detector.widget_calls:
                # Only include if this is a standalone expression, not part of an assignment
                if not call_info.get('is_assignment'):
                    # Try to evaluate the call if it's safe
                    try:
                        # Use globals only, as locals might not be available
                        result = eval(call_info['code'], self.session.globals, {})
                        if self._is_marimo_widget(result):
                            widget_object_id = id(result)
                            
                            # Skip if already processed
                            if widget_object_id not in processed_widgets:
                                processed_widgets.add(widget_object_id)
                                widget_result = self._format_widget_result(result)
                                widgets.append(widget_result)
                    except:
                        pass  # Skip unsafe evaluations
            
        except (SyntaxError, ValueError):
            # If AST parsing fails, fall back to simple widget detection
            pass
        
        return widgets

class WidgetDetectorVisitor(ast.NodeVisitor):
    """AST visitor to detect marimo widget patterns"""
    
    def __init__(self, session):
        self.session = session
        self.widget_assignments = {}
        self.widget_calls = []
        self.current_assignment_target = None
        self.in_assignment = False
    
    def visit_Assign(self, node):
        """Visit assignment nodes to detect widget assignments"""
        # Handle simple assignments like: widget = mo.ui.slider(...)
        if len(node.targets) == 1 and isinstance(node.targets[0], ast.Name):
            target_name = node.targets[0].id
            self.current_assignment_target = target_name
            self.in_assignment = True
            
            # Check if the value is a widget call
            if self._is_widget_call(node.value):
                self.widget_assignments[target_name] = node.value
        
        self.generic_visit(node)
        self.in_assignment = False
        self.current_assignment_target = None
    
    def visit_Expr(self, node):
        """Visit expression statements to detect standalone widget expressions"""
        # This handles standalone expressions like just "mo.ui.slider()" on its own line
        if self._is_widget_call(node.value):
            call_code = ast.unparse(node.value) if hasattr(ast, 'unparse') else self._unparse_call(node.value)
            self.widget_calls.append({
                'code': call_code,
                'node': node.value,
                'is_assignment': False  # This is a standalone expression
            })
        
        self.generic_visit(node)
    
    def visit_Call(self, node):
        """Visit function calls to detect widget creation"""
        if self._is_widget_call(node):
            call_code = ast.unparse(node) if hasattr(ast, 'unparse') else self._unparse_call(node)
            # Only add if this call is not already handled by visit_Expr or visit_Assign
            if not self.in_assignment:
                self.widget_calls.append({
                    'code': call_code,
                    'node': node,
                    'is_assignment': False
                })
        
        self.generic_visit(node)
    
    def _is_widget_call(self, node):
        """Check if a call node represents a widget creation"""
        if not isinstance(node, ast.Call):
            return False
        
        # Check for mo.ui.* calls
        if isinstance(node.func, ast.Attribute):
            # Handle mo.ui.slider(), mo.ui.button(), etc.
            if (isinstance(node.func.value, ast.Attribute) and 
                isinstance(node.func.value.value, ast.Name) and
                node.func.value.value.id == 'mo' and
                node.func.value.attr == 'ui'):
                return True
            
            # Handle direct ui.slider() calls (if ui is imported)
            if (isinstance(node.func.value, ast.Name) and
                node.func.value.id == 'ui'):
                return True
        
        # Check for direct widget function calls
        if isinstance(node.func, ast.Name):
            widget_functions = ['slider', 'button', 'text', 'checkbox', 'dropdown', 'select', 'radio', 'multiselect', 'number']
            if node.func.id in widget_functions:
                return True
        
        return False
    
    def _unparse_call(self, node):
        """Fallback unparsing for older Python versions"""
        try:
            if isinstance(node.func, ast.Attribute):
                if isinstance(node.func.value, ast.Attribute):
                    # mo.ui.slider format
                    if isinstance(node.func.value.value, ast.Name):
                        return f"{node.func.value.value.id}.{node.func.value.attr}.{node.func.attr}()"
                else:
                    # ui.slider format
                    if isinstance(node.func.value, ast.Name):
                        return f"{node.func.value.id}.{node.func.attr}()"
            elif isinstance(node.func, ast.Name):
                # slider format
                return f"{node.func.id}()"
        except AttributeError:
            pass
        return "unknown_widget_call()"
