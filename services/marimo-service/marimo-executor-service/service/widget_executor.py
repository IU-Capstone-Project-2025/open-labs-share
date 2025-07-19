import sys
import traceback
import importlib
import subprocess
import logging
import time
from typing import Dict, Any, List
from contextlib import contextmanager
from io import StringIO

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class DependencyError(Exception):
    """Custom exception for dependency-related errors"""
    pass

class ExecutionError(Exception):
    """Custom exception for code execution errors"""
    pass

class WidgetError(Exception):
    """Custom exception for widget-specific errors"""
    pass

class ErrorClassifier:
    """Centralized error handling and classification"""
    
    ERROR_CATEGORIES = {
        'DEPENDENCY': 'dependency',
        'SYNTAX': 'syntax',
        'RUNTIME': 'runtime',
        'WIDGET': 'widget',
        'SYSTEM': 'system',
        'NETWORK': 'network',
        'MEMORY': 'memory',
        'TIMEOUT': 'timeout'
    }
    
    SEVERITY_LEVELS = {
        'LOW': 'low',
        'MEDIUM': 'medium',
        'HIGH': 'high',
        'CRITICAL': 'critical'
    }
    
    @classmethod
    def categorize_error(cls, error: Exception) -> Dict[str, Any]:
        """Categorize error and determine severity"""
        error_type = type(error).__name__
        error_message = str(error)
        
        # Dependency errors
        if isinstance(error, (ImportError, ModuleNotFoundError, DependencyError)):
            return {
                'category': cls.ERROR_CATEGORIES['DEPENDENCY'],
                'severity': cls.SEVERITY_LEVELS['HIGH'],
                'recoverable': True,
                'auto_retry': True
            }
        
        # Syntax errors
        elif isinstance(error, SyntaxError):
            return {
                'category': cls.ERROR_CATEGORIES['SYNTAX'],
                'severity': cls.SEVERITY_LEVELS['MEDIUM'],
                'recoverable': False,
                'auto_retry': False
            }
        
        # Memory errors
        elif isinstance(error, MemoryError):
            return {
                'category': cls.ERROR_CATEGORIES['MEMORY'],
                'severity': cls.SEVERITY_LEVELS['CRITICAL'],
                'recoverable': True,
                'auto_retry': False
            }
        
        # Widget-specific errors
        elif isinstance(error, WidgetError):
            return {
                'category': cls.ERROR_CATEGORIES['WIDGET'],
                'severity': cls.SEVERITY_LEVELS['MEDIUM'],
                'recoverable': True,
                'auto_retry': True
            }
        
        # Default runtime error
        else:
            return {
                'category': cls.ERROR_CATEGORIES['RUNTIME'],
                'severity': cls.SEVERITY_LEVELS['MEDIUM'],
                'recoverable': True,
                'auto_retry': True
            }
    
    @classmethod
    def should_retry(cls, error: Exception, attempt: int, max_attempts: int = 3) -> bool:
        """Determine if error should trigger a retry"""
        if attempt >= max_attempts:
            return False
        
        error_info = cls.categorize_error(error)
        return error_info['auto_retry'] and error_info['recoverable']


class PackageManager:
    """Handles package installation and validation"""
    
    WIDGET_DEPENDENCIES = {
        'plotly': ['plotly>=5.0.0'],
        'matplotlib': ['matplotlib>=3.0.0'],
        'seaborn': ['seaborn>=0.11.0'],
        'pandas': ['pandas>=1.0.0'],
        'numpy': ['numpy>=1.19.0'],
        'scipy': ['scipy>=1.5.0'],
        'sklearn': ['scikit-learn>=0.24.0'],
        'marimo': ['marimo>=0.1.0']
    }
    
    @classmethod
    def detect_required_packages(cls, code: str) -> List[str]:
        """Detect required packages from code"""
        required = []
        
        # Check for common import patterns
        import_patterns = [
            ('import plotly', 'plotly'),
            ('import matplotlib', 'matplotlib'),
            ('import seaborn', 'seaborn'),
            ('import pandas', 'pandas'),
            ('import numpy', 'numpy'),
            ('import scipy', 'scipy'),
            ('import sklearn', 'sklearn'),
            ('import marimo', 'marimo'),
            ('from plotly', 'plotly'),
            ('from matplotlib', 'matplotlib'),
            ('from seaborn', 'seaborn'),
            ('from pandas', 'pandas'),
            ('from numpy', 'numpy'),
            ('from scipy', 'scipy'),
            ('from sklearn', 'sklearn'),
            ('from marimo', 'marimo')
        ]
        
        for pattern, package in import_patterns:
            if pattern in code:
                required.append(package)
        
        return list(set(required))
    
    @classmethod
    def install_package(cls, package: str) -> bool:
        """Install a package using pip"""
        try:
            subprocess.run([
                sys.executable, '-m', 'pip', 'install', package
            ], check=True, capture_output=True, text=True)
            logger.info(f"Successfully installed package: {package}")
            return True
        except subprocess.CalledProcessError as e:
            logger.error(f"Failed to install package {package}: {e}")
            return False
    
    @classmethod
    def validate_dependencies(cls, packages: List[str]) -> Dict[str, bool]:
        """Validate that required packages are available"""
        validation_results = {}
        
        for package in packages:
            try:
                importlib.import_module(package)
                validation_results[package] = True
            except ImportError:
                validation_results[package] = False
                
                # Attempt to install missing package
                if package in cls.WIDGET_DEPENDENCIES:
                    for dep in cls.WIDGET_DEPENDENCIES[package]:
                        if cls.install_package(dep):
                            validation_results[package] = True
                            break
        
        return validation_results


class ExecutionManager:
    """Manages code execution with timeout and error handling"""
    
    def __init__(self, timeout: int = 30):
        self.timeout = timeout
        self.execution_stats = {
            'total_executions': 0,
            'successful_executions': 0,
            'failed_executions': 0,
            'retries': 0
        }
    
    @contextmanager
    def capture_output(self):
        """Context manager to capture stdout and stderr"""
        old_stdout, old_stderr = sys.stdout, sys.stderr
        stdout_capture = StringIO()
        stderr_capture = StringIO()
        
        try:
            sys.stdout, sys.stderr = stdout_capture, stderr_capture
            yield stdout_capture, stderr_capture
        finally:
            sys.stdout, sys.stderr = old_stdout, old_stderr
    
    def execute_with_retry(self, code: str, context: Dict[str, Any] = None, 
                          max_attempts: int = 3) -> Dict[str, Any]:
        """Execute code with retry logic"""
        if context is None:
            context = {}
        
        for attempt in range(max_attempts):
            try:
                result = self.execute_code(code, context)
                self.execution_stats['successful_executions'] += 1
                return result
            except Exception as e:
                self.execution_stats['failed_executions'] += 1
                
                if ErrorClassifier.should_retry(e, attempt, max_attempts):
                    self.execution_stats['retries'] += 1
                    time.sleep(2 ** attempt)  # Exponential backoff
                    continue
                else:
                    raise
        
        raise ExecutionError(f"Code execution failed after {max_attempts} attempts")
    
    def execute_code(self, code: str, context: Dict[str, Any] = None) -> Dict[str, Any]:
        """Execute Python code with error handling"""
        if context is None:
            context = {}
        
        self.execution_stats['total_executions'] += 1
        
        # Detect and validate dependencies
        required_packages = PackageManager.detect_required_packages(code)
        validation_results = PackageManager.validate_dependencies(required_packages)
        
        # Check for missing dependencies
        missing_deps = [pkg for pkg, valid in validation_results.items() if not valid]
        if missing_deps:
            raise DependencyError(f"Missing required packages: {', '.join(missing_deps)}")
        
        # Execute code with output capture
        with self.capture_output() as (stdout, stderr):
            try:
                # Create execution context
                exec_context = {
                    '__builtins__': __builtins__,
                    'marimo': self._create_marimo_context(),
                    **context
                }
                
                # Execute the code
                exec(code, exec_context)
                
                # Extract any widget outputs
                widget_outputs = self._extract_widget_outputs(exec_context)
                
                return {
                    'success': True,
                    'stdout': stdout.getvalue(),
                    'stderr': stderr.getvalue(),
                    'widget_outputs': widget_outputs,
                    'execution_time': time.time(),
                    'context': exec_context
                }
                
            except Exception as e:
                error_info = ErrorClassifier.categorize_error(e)
                
                return {
                    'success': False,
                    'error': str(e),
                    'error_type': type(e).__name__,
                    'error_category': error_info['category'],
                    'error_severity': error_info['severity'],
                    'traceback': traceback.format_exc(),
                    'stdout': stdout.getvalue(),
                    'stderr': stderr.getvalue(),
                    'recoverable': error_info['recoverable']
                }
    
    def _create_marimo_context(self) -> Dict[str, Any]:
        """Create Marimo widget context"""
        return {
            'slider': self._create_slider_widget,
            'number': self._create_number_widget,
            'dropdown': self._create_dropdown_widget,
            'radio': self._create_radio_widget,
            'switch': self._create_switch_widget,
            'textarea': self._create_textarea_widget,
            'range': self._create_range_widget,
            'multiselect': self._create_multiselect_widget,
            'button': self._create_button_widget,
            'table': self._create_table_widget,
            'plotly': self._create_plotly_widget
        }
    
    def _extract_widget_outputs(self, context: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Extract widget outputs from execution context"""
        widget_outputs = []
        
        for key, value in context.items():
            if key.startswith('_marimo_widget_'):
                widget_outputs.append({
                    'widget_id': key,
                    'widget_type': value.get('type', 'unknown'),
                    'widget_data': value.get('data', {}),
                    'widget_config': value.get('config', {})
                })
        
        return widget_outputs
    
    # Widget factory methods
    def _create_slider_widget(self, min_val=0, max_val=100, step=1, value=50, **kwargs):
        """Create slider widget configuration"""
        return {
            'type': 'slider',
            'data': {'value': value},
            'config': {
                'min': min_val,
                'max': max_val,
                'step': step,
                **kwargs
            }
        }
    
    def _create_number_widget(self, value=0, min_val=None, max_val=None, **kwargs):
        """Create number input widget configuration"""
        return {
            'type': 'number',
            'data': {'value': value},
            'config': {
                'min': min_val,
                'max': max_val,
                **kwargs
            }
        }
    
    def _create_dropdown_widget(self, options=None, value=None, **kwargs):
        """Create dropdown widget configuration"""
        if options is None:
            options = []
        
        return {
            'type': 'dropdown',
            'data': {'value': value},
            'config': {
                'options': options,
                **kwargs
            }
        }
    
    def _create_radio_widget(self, options=None, value=None, **kwargs):
        """Create radio button widget configuration"""
        if options is None:
            options = []
        
        return {
            'type': 'radio',
            'data': {'value': value},
            'config': {
                'options': options,
                **kwargs
            }
        }
    
    def _create_switch_widget(self, value=False, **kwargs):
        """Create switch widget configuration"""
        return {
            'type': 'switch',
            'data': {'value': value},
            'config': kwargs
        }
    
    def _create_textarea_widget(self, value='', placeholder='', **kwargs):
        """Create textarea widget configuration"""
        return {
            'type': 'textarea',
            'data': {'value': value},
            'config': {
                'placeholder': placeholder,
                **kwargs
            }
        }
    
    def _create_range_widget(self, min_val=0, max_val=100, step=1, value=None, **kwargs):
        """Create range slider widget configuration"""
        if value is None:
            value = [min_val, max_val]
        
        return {
            'type': 'range',
            'data': {'value': value},
            'config': {
                'min': min_val,
                'max': max_val,
                'step': step,
                **kwargs
            }
        }
    
    def _create_multiselect_widget(self, options=None, value=None, **kwargs):
        """Create multiselect widget configuration"""
        if options is None:
            options = []
        if value is None:
            value = []
        
        return {
            'type': 'multiselect',
            'data': {'value': value},
            'config': {
                'options': options,
                **kwargs
            }
        }
    
    def _create_button_widget(self, label='Click me', **kwargs):
        """Create button widget configuration"""
        return {
            'type': 'button',
            'data': {'clicked': False},
            'config': {
                'label': label,
                **kwargs
            }
        }
    
    def _create_table_widget(self, data=None, columns=None, **kwargs):
        """Create table widget configuration"""
        if data is None:
            data = []
        if columns is None:
            columns = []
        
        return {
            'type': 'table',
            'data': {'rows': data},
            'config': {
                'columns': columns,
                **kwargs
            }
        }
    
    def _create_plotly_widget(self, data=None, layout=None, config=None, **kwargs):
        """Create Plotly widget configuration"""
        if data is None:
            data = []
        if layout is None:
            layout = {}
        if config is None:
            config = {}
        
        return {
            'type': 'plotly',
            'data': {'traces': data},
            'config': {
                'layout': layout,
                'config': config,
                **kwargs
            }
        }


class MarimoWidgetExecutor:
    """Main executor class for Marimo widgets"""
    
    def __init__(self, timeout: int = 30):
        self.execution_manager = ExecutionManager(timeout)
        self.error_classifier = ErrorClassifier()
    
    def execute_widget_code(self, code: str, widget_type: str = None, 
                           context: Dict[str, Any] = None) -> Dict[str, Any]:
        """Execute widget code with comprehensive error handling"""
        try:
            result = self.execution_manager.execute_with_retry(code, context)
            
            # Add widget-specific metadata
            if widget_type:
                result['widget_type'] = widget_type
            
            return result
            
        except Exception as e:
            error_info = self.error_classifier.categorize_error(e)
            
            return {
                'success': False,
                'error': str(e),
                'error_type': type(e).__name__,
                'error_category': error_info['category'],
                'error_severity': error_info['severity'],
                'traceback': traceback.format_exc(),
                'recoverable': error_info['recoverable'],
                'widget_type': widget_type
            }
    
    def get_execution_stats(self) -> Dict[str, Any]:
        """Get execution statistics"""
        return {
            **self.execution_manager.execution_stats,
            'success_rate': (
                self.execution_manager.execution_stats['successful_executions'] /
                max(self.execution_manager.execution_stats['total_executions'], 1)
            ) * 100
        }


# Example usage
if __name__ == "__main__":
    executor = MarimoWidgetExecutor()
    
    # Example widget code
    widget_code = """
import marimo as mo
import plotly.graph_objects as go

# Create a sample chart
fig = go.Figure(data=go.Bar(x=['A', 'B', 'C'], y=[1, 3, 2]))
fig.update_layout(title='Sample Bar Chart')

# Create a Plotly widget
chart_widget = mo.plotly(fig)
"""
    
    result = executor.execute_widget_code(widget_code, 'plotly')
    
    if result['success']:
        # Widget executed successfully - no logging needed
        pass
    else:
        # Widget execution failed - could log errors if needed
        pass
