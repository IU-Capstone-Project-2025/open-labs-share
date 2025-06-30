# Import downloaded modules
import grpc
import pytest

# Import built-in modules
import os
import sys
import logging
sys.path.append(os.path.join(os.path.dirname(__file__), "proto"))

# Import project files
from config import Config
import client
import proto.labs_service_pb2 as labs_service_cf
import proto.labs_service_pb2_grpc as labs_service_cf_grpc
import proto.submissions_service_pb2 as submissions_service_cf
import proto.submissions_service_pb2_grpc as submissions_service_cf_grpc

logger = logging.getLogger("Tester")
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

def check_CreateLab(stub, number):
    response = client.CreateLab(stub, number)
    logger.info(response)
    expected = {
        "owner_id": 1,
        "title": f"Lab{number}",
        "abstract": f"Lab{number}",
        "views": 0,
        "submissions": 0,
        "stars_total": 0,
        "people_rated": 0,
        "related_articles": labs_service_cf.ArticleList(total_count=0)
    }

    print()

    for key, value in expected.items():
        print(f"hasattr(response, '{key}')={hasattr(response, key)}")
        if not hasattr(response, key):
            return False, f"Response doesn't has {key} attribute!"

        print(f"getattr(response, '{key}')=={value}: {getattr(response, key) == value}")
        if not getattr(response, key) == value:
            return False, f"Response attribute={key} not equals {value}!"

    return True, ""


def test():
    address = f"{Config.SERVICE_HOST}:{Config.SERVICE_PORT}"

    with grpc.insecure_channel(address) as channel:
        labs_service_stub = labs_service_cf_grpc.LabServiceStub(channel)

        res = check_CreateLab(labs_service_stub, 1)
        assert res[0], res[1]