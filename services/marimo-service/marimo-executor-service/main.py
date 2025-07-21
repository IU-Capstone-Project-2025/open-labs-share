import os
import sys
import grpc
from concurrent import futures

# Add the generated gRPC code directory to Python path
sys.path.append(os.path.join(os.path.dirname(__file__), 'proto'))

from config import Config
from service.session import SessionManager
from service.executor import MarimoCellExecutor
from service.logging_config import setup_logging, get_logger

# Import generated gRPC code
import marimo_executor_service_pb2 as marimo_service_pb2
import marimo_executor_service_pb2_grpc as marimo_service_pb2_grpc

class MarimoExecutorService(marimo_service_pb2_grpc.MarimoExecutorServicer):
    def __init__(self):
        self.session_manager = SessionManager()
        self.logger = get_logger("grpc_service")

    def _map_output_type(self, output_type_str):
        """Map string output type to protobuf enum."""
        type_mapping = {
            'TEXT': marimo_service_pb2.CellOutput.OutputType.TEXT,
            'STDOUT': marimo_service_pb2.CellOutput.OutputType.STDOUT,
            'STDERR': marimo_service_pb2.CellOutput.OutputType.STDERR,
            'EXPRESSION_RESULT': marimo_service_pb2.CellOutput.OutputType.EXPRESSION_RESULT,
            'ERROR': marimo_service_pb2.CellOutput.OutputType.ERROR,
            'HTML': marimo_service_pb2.CellOutput.OutputType.HTML,
            'PLOT': marimo_service_pb2.CellOutput.OutputType.PLOT,
            'WIDGET': marimo_service_pb2.CellOutput.OutputType.WIDGET,
        }
        return type_mapping.get(output_type_str, marimo_service_pb2.CellOutput.OutputType.TEXT)

    def _map_data_type(self, data_type_str):
        """Map string data type to protobuf enum."""
        type_mapping = {
            'TEXT': marimo_service_pb2.CellOutput.DataType.TEXT_DATA,
            'HTML': marimo_service_pb2.CellOutput.DataType.HTML_DATA,
            'JSON': marimo_service_pb2.CellOutput.DataType.JSON_DATA,
            'IMAGE': marimo_service_pb2.CellOutput.DataType.IMAGE_DATA,
        }
        return type_mapping.get(data_type_str, marimo_service_pb2.CellOutput.DataType.TEXT_DATA)

    def StartSession(self, request, context):
        try:
            # Extract component_id from request if provided
            component_id = request.component_id if request.HasField('component_id') else None
            # Pass the session_id and component_id from the request to the session manager
            session_id, session = self.session_manager.create_session(
                request.session_id, 
                request.notebook_path, 
                component_id
            )
            return marimo_service_pb2.StartSessionResponse(
                success=True,
                error=""
            )
        except Exception as e:
            self.logger.error(f"Failed to start session {request.session_id}: {e}", exc_info=True)
            return marimo_service_pb2.StartSessionResponse(
                success=False,
                error=str(e)
            )

    def ExecuteCell(self, request, context):
        try:
            session = self.session_manager.get_session(request.session_id)
            if not session:
                return marimo_service_pb2.ExecuteResponse(
                    success=False,
                    error="Session not found"
                )

            executor = MarimoCellExecutor(session)
            success, outputs, error, cell_state = executor.execute_cell(
                request.cell_id,
                request.code
            )

            # Convert outputs to protobuf format
            proto_outputs = []
            for output in outputs:
                # Map output types to protobuf enums
                output_type = self._map_output_type(output.get('type', 'TEXT'))
                data_type = self._map_data_type(output.get('data_type', 'TEXT'))
                
                proto_output = marimo_service_pb2.CellOutput(
                    type=output_type,
                    content=output.get('content', ''),
                    data=output.get('data', b''),
                    mime_type=output.get('mime_type', 'text/plain'),
                    metadata=output.get('metadata', {}),
                    data_type=data_type
                )
                proto_outputs.append(proto_output)

            return marimo_service_pb2.ExecuteResponse(
                success=success,
                outputs=proto_outputs,
                error=error,
                cell_state=cell_state
            )

        except Exception as e:
            return marimo_service_pb2.ExecuteResponse(
                success=False,
                error=str(e)
            )

    def EndSession(self, request, context):
        try:
            self.session_manager.end_session(request.session_id)
            return marimo_service_pb2.EndSessionResponse(
                success=True,
                error=""
            )
        except Exception as e:
            return marimo_service_pb2.EndSessionResponse(
                success=False,
                error=str(e)
            )

    def GetSessionState(self, request, context):
        try:
            session = self.session_manager.get_session(request.session_id)
            if not session:
                return marimo_service_pb2.SessionStateResponse(
                    exists=False,
                    state={}
                )

            return marimo_service_pb2.SessionStateResponse(
                exists=True,
                state=session.get_state()
            )
        except Exception as e:
            return marimo_service_pb2.SessionStateResponse(
                exists=False,
                state={}
            )
    
    def UpdateWidgetValue(self, request, context):
        """Update widget value in the session"""
        try:
            session = self.session_manager.get_session(request.session_id)
            if not session:
                return marimo_service_pb2.UpdateWidgetValueResponse(
                    success=False,
                    error="Session not found"
                )
            
            # Parse the value based on widget type
            widget_value = request.value
            
            # Get widget info to determine type
            widget_info = session.widgets.get(request.widget_id)
            widget_type = widget_info['type'] if widget_info else 'unknown'
            
            # Try to parse JSON for complex values
            try:
                import json
                widget_value = json.loads(request.value)
            except (json.JSONDecodeError, ValueError):
                # If not valid JSON, keep as string
                widget_value = request.value
            
            # Type-specific validation and conversion
            if widget_type == 'number':
                try:
                    if widget_value is None or widget_value == '':
                        widget_value = 0
                    elif isinstance(widget_value, str):
                        widget_value = float(widget_value)
                    elif not isinstance(widget_value, (int, float)):
                        widget_value = float(widget_value)
                except (ValueError, TypeError):
                    # If conversion fails, use default value or previous value
                    widget_value = widget_info.get('value', 0) if widget_info else 0
                    self.logger.warning(f"Invalid number value for widget {request.widget_id}, using default: {widget_value}")
            
            elif widget_type == 'checkbox':
                widget_value = bool(widget_value)
            
            elif widget_type in ['dropdown', 'radio']:
                # Single selection widgets - ensure string value
                if widget_value is None:
                    widget_value = ''
                else:
                    widget_value = str(widget_value)
            
            elif widget_type == 'multiselect':
                # Multi-selection widget - ensure list
                if not isinstance(widget_value, list):
                    if widget_value is None:
                        widget_value = []
                    else:
                        widget_value = [widget_value]  # Wrap single value in list
            
            elif widget_type == 'range_slider':
                if not isinstance(widget_value, list) or len(widget_value) != 2:
                    widget_value = [0, 100]  # Default range
            
            # Update widget value in session
            session.update_widget_value(request.widget_id, widget_value)
            
            self.logger.info(f"Updated widget {request.widget_id} to value: {widget_value}")
            
            return marimo_service_pb2.UpdateWidgetValueResponse(
                success=True,
                error=""
            )
            
        except Exception as e:
            self.logger.error(f"Failed to update widget {request.widget_id}: {e}", exc_info=True)
            return marimo_service_pb2.UpdateWidgetValueResponse(
                success=False,
                error=str(e)
            )

def serve():
    # Setup logging configuration
    logger = setup_logging(
        log_level=os.getenv("LOG_LEVEL", "INFO"),
        log_file=os.getenv("LOG_FILE", None)
    )

    # Create gRPC server
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    marimo_service_pb2_grpc.add_MarimoExecutorServicer_to_server(
        MarimoExecutorService(), server
    )

    # Add secure credentials if needed
    server.add_insecure_port(f'[::]:{Config.GRPC_PORT}')

    # Start server
    server.start()
    logger.info(f'Marimo Python Service started on port {Config.GRPC_PORT}')

    # Keep alive
    server.wait_for_termination()

if __name__ == '__main__':
    serve()
