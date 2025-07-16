# Import downloaded modules
import grpc

# Import built-in modules
import sys
import os
import logging
import time
from concurrent import futures

# Fixes import path for files
sys.path.append(os.path.join(os.path.dirname(__file__), "proto"))
sys.path.append(os.path.join(os.path.dirname(__file__), "services"))

# Import project files
from config import Config
from services.labs_service import LabService
from services.submissions_service import SubmissionService
from services.tags_service import TagService
import proto.labs_service_pb2_grpc as labs_service # Generated from labs.proto
import proto.submissions_service_pb2_grpc as submissions_service  # Generated from submissions_service.proto
import proto.tags_service_pb2_grpc as tags_service  # Generated from tags.proto
from grpc_health.v1 import health
from grpc_health.v1 import health_pb2
from grpc_health.v1 import health_pb2_grpc


logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

if __name__ == "__main__":
    logger = logging.getLogger("__main__")
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))

    labs_service.add_LabServiceServicer_to_server(LabService(), server)
    submissions_service.add_SubmissionServiceServicer_to_server(SubmissionService(), server)
    tags_service.add_TagServiceServicer_to_server(TagService(), server)

    # Add HealthServicer to the server
    health_servicer = health.HealthServicer()
    health_pb2_grpc.add_HealthServicer_to_server(health_servicer, server)
    health_servicer.set("labs.LabService", health_pb2.HealthCheckResponse.SERVING)
    health_servicer.set("submissions.SubmissionService", health_pb2.HealthCheckResponse.SERVING)
    health_servicer.set("tags.TagService", health_pb2.HealthCheckResponse.SERVING)


    server_address = f"{Config.SERVICE_HOST}:{Config.SERVICE_PORT}"
    server.add_insecure_port(server_address)
    logger.info(f"Starting gRPC server on {server_address}")
    server.start()
    try:
        server.wait_for_termination()
    except Exception as e:
        logger.info("Server is shutting down...")
        logger.error(f"Server shutdown due to: {e}")
        server.stop(0)
        sys.exit(0)
