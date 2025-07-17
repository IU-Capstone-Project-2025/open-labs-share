import io
import gzip
from typing import Any, Dict
import numpy as np
import plotly.graph_objects as go
from matplotlib import pyplot as plt
from config import Config

class PlotOptimizer:
    def __init__(self):
        self.webgl_threshold = Config.WEBGL_THRESHOLD

    def optimize_plot(self, plot: Any) -> Dict[str, Any]:
        """Optimize plot based on its type and size"""
        if isinstance(plot, plt.Figure):
            return self._optimize_matplotlib(plot)
        elif isinstance(plot, go.Figure):
            return self._optimize_plotly(plot)
        return None

    def _optimize_matplotlib(self, fig: plt.Figure) -> Dict[str, Any]:
        """Optimize matplotlib figure"""
        buffer = io.BytesIO()
        fig.savefig(buffer, format='png', dpi='figure', bbox_inches='tight')
        data = buffer.getvalue()
        
        return {
            'type': 'PLOT',
            'data': self._compress_if_needed(data),
            'mime_type': 'image/png'
        }

    def _optimize_plotly(self, fig: go.Figure) -> Dict[str, Any]:
        """Optimize plotly figure"""
        # Convert to WebGL for large datasets
        if self._should_use_webgl(fig):
            fig = self._convert_to_webgl(fig)
        
        return {
            'type': 'PLOT',
            'content': fig.to_json(),
            'mime_type': 'application/json'
        }

    def _should_use_webgl(self, fig: go.Figure) -> bool:
        """Determine if WebGL should be used"""
        total_points = 0
        for trace in fig.data:
            if hasattr(trace, 'x'):
                total_points += len(trace.x)
        return total_points > self.webgl_threshold

    def _convert_to_webgl(self, fig: go.Figure) -> go.Figure:
        """Convert compatible traces to WebGL"""
        new_data = []
        for trace in fig.data:
            if trace.type == 'scatter':
                trace.type = 'scattergl'
            new_data.append(trace)
        fig.data = new_data
        return fig

    def _compress_if_needed(self, data: bytes) -> bytes:
        """Compress data if it exceeds threshold"""
        if len(data) > 1_000_000:  # 1MB
            return gzip.compress(data)
        return data

class OutputBuffer:
    def __init__(self):
        self.max_size = Config.MAX_OUTPUT_SIZE_MB * 1024 * 1024
        self.plot_optimizer = PlotOptimizer()

    def process_output(self, output: Any) -> Dict[str, Any]:
        """Process and optimize different types of outputs"""
        if isinstance(output, (plt.Figure, go.Figure)):
            return self.plot_optimizer.optimize_plot(output)
        elif isinstance(output, (np.ndarray, list)):
            return self._process_data(output)
        elif isinstance(output, str):
            return self._process_text(output)
        elif isinstance(output, dict):
            return self._process_dict(output)
        return None

    def _process_data(self, data: Any) -> Dict[str, Any]:
        """Process numerical data"""
        if isinstance(data, np.ndarray):
            data = data.tolist()
        return {
            'type': 'TEXT',
            'content': str(data),
            'mime_type': 'text/plain'
        }

    def _process_text(self, text: str) -> Dict[str, Any]:
        """Process text output"""
        return {
            'type': 'TEXT',
            'content': text,
            'mime_type': 'text/plain'
        }

    def _process_dict(self, data: dict) -> Dict[str, Any]:
        """Process dictionary output"""
        import json
        return {
            'type': 'TEXT',
            'content': json.dumps(data),
            'mime_type': 'application/json'
        }
