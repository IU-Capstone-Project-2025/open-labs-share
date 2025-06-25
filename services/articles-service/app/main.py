# Import downloaded modules
import grpc
from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session
import minio

# Import built-in modules
import os
from concurrent import futures

# Import project files
from config import Config
import proto.articles_pb2 as stub # Generated from labs.proto
import proto.articles_pb2_grpc as service # Generated from labs.proto
from utils.models import Article, ArticleAsset


class ArticleService(service.ArticleServiceServicer):
    def __init__(self, config: Config):
        user = Config.DB_USER
        password = Config.DB_PASSWORD
        host = Config.DB_HOST
        port = Config.DB_PORT
        db_name = Config.DB_NAME
        url = f"postgresql://{user}:{password}@{host}:{port}/{db_name}"

        self.engine = create_engine(url, echo=False)
        self.minio_client = minio.Minio(
            endpoint=Config.MINIO_ENDPOINT,
            access_key=Config.MINIO_ACCESS_KEY,
            secret_key=Config.MINIO_SECRET_KEY,
            secure=False
        )

        if not self.minio_client.bucket_exists("labs"):
            self.minio_client.make_bucket("labs")

        # Ensure the temporary files directory exists
        if not os.path.exists('files'):
            os.makedirs('files')

    # Articles Management
    def CreateArticle(self, request, context):
        pass

    def GetArticle(self, request, context):
        pass

    def GetArticles(self, request, context):
        pass

    def UpdateArticle(self, request, context):
        pass

    def DeleteArticle(self, request, context):
        pass

    # Assets Management
    def UploadAsset(self, request_iterator, context):
        pass

    def UpdateAsset(self, request_iterator, context):
        pass

    def DownloadAsset(self, request, context):
        pass

    def DeleteAsset(self, request, context):
        pass

    def ListAssets(self, request, context):
        pass


if __name__ == "__main__":
    config = Config()

    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    service.add_ArticleServiceServicer_to_server(ArticleService(config), server)

    server.add_insecure_port(f"{config.SERVICE_HOST}:{config.SERVICE_PORT}")
    server.start()

    print(f"Server started on {config.SERVICE_HOST}:{config.SERVICE_PORT}")
    server.wait_for_termination()
