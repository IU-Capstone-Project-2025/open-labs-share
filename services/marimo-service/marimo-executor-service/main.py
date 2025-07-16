import os
import sys
import grpc
from concurrent import futures
import logging

# Add the generated gRPC code directory to Python path
sys.path.append(os.path.join(os.path.dirname(__file__), 'proto'))

from config import Config
from service.session import SessionManager
from service.executor import MarimoCellExecutor

# Import generated gRPC code
import marimo_executor_service_pb2 as marimo_service_pb2
import marimo_executor_service_pb2_grpc as marimo_service_pb2_grpc

class MarimoExecutorService(marimo_service_pb2_grpc.MarimoExecutorServicer):
    def __init__(self):
        self.session_manager = SessionManager()

    def StartSession(self, request, context):
        try:
            # Pass the session_id from the request to the session manager
            session_id, session = self.session_manager.create_session(request.session_id, request.notebook_path)
            return marimo_service_pb2.StartSessionResponse(
                success=True,
                error=""
            )
        except Exception as e:
            logging.error(f"Failed to start session {request.session_id}: {e}", exc_info=True)
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
                proto_output = marimo_service_pb2.CellOutput(
                    type=output.get('type', 'TEXT'),
                    content=output.get('content', ''),
                    data=output.get('data', b''),
                    mime_type=output.get('mime_type', 'text/plain'),
                    metadata=output.get('metadata', {})
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

def serve():
    # Configure logging
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(levelname)s - %(message)s'
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
    logging.info(f'Marimo Python Service started on port {Config.GRPC_PORT}')

    # Keep alive
    server.wait_for_termination()

if __name__ == '__main__':
    serve()
