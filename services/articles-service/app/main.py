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
    def __init__(self):
        user = Config.DB_USER
        password = Config.DB_PASSWORD
        host = Config.DB_HOST
        port = Config.DB_PORT
        db_name = Config.DB_NAME
        url = f"postgresql://{user}:{password}@{host}:{port}/{db_name}"

        self.engine = create_engine(url, echo=True)
        self.minio_client = minio.Minio(
            endpoint=Config.MINIO_ENDPOINT,
            access_key=Config.MINIO_ACCESS_KEY,
            secret_key=Config.MINIO_SECRET_KEY,
            secure=False
        )

        if not self.minio_client.bucket_exists("articles"):
            self.minio_client.make_bucket("articles")

        # Ensure the temporary files directory exists
        if not os.path.exists('files'):
            os.makedirs('files')

    # Articles Management
    def CreateArticle(self, request, context):
        data: dict = {
            "owner_id": request.owner_id,
            "title": request.title,
            "abstract": request.abstract
        }

        with Session(self.engine) as session:
            article = Article(**data)
            session.add(article)
            session.commit()
            session.refresh(article)

            return stub.Article(**article.get_attrs())

    def GetArticle(self, request, context) -> stub.Article:
        data: dict = {
            "article_id": request.article_id
        }

        with Session(self.engine) as session:
            stmt = select(Article).where(Article.id == data["article_id"])
            article = session.execute(stmt).scalar_one_or_none()

            if article is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Article not found")
                return stub.Article()

            return stub.Article(**article.get_attrs())

    def GetArticles(self, request, context) -> stub.ArticleList:
        data: dict = {
            "page_number": request.page_number,
            "page_size": request.page_size
        }

        with Session(self.engine) as session:
            stmt = select(Article).offset((data["page_number"] - 1) * data["page_size"]).limit(data["page_size"])
            articles = session.execute(stmt).scalars().all()

            article_list = stub.ArticleList(total_count=len(articles))
            for article in articles:
                article_list.articles.append(stub.Article(**article.get_attrs()))

            return article_list

    def UpdateArticle(self, request, context) -> stub.Article:
        data: dict = {
            "article_id": request.article_id,
            "title": request.title if request.HasField("title") else None,
            "abstract": request.abstract if request.HasField("abstract") else None
        }

        with Session(self.engine) as session:
            stmt = select(Article).where(Article.id == data["article_id"])
            article = session.execute(stmt).scalar_one_or_none()

            if article is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Article not found")
                return stub.Article()

            if data["title"] is not None:
                article.title = data["title"]

            if data["abstract"] is not None:
                article.abstract = data["abstract"]

            session.commit()

            return stub.Article(**article.get_attrs())

    def DeleteArticle(self, request, context) -> stub.DeleteArticleResponse:
        data: dict = {
            "article_id": request.article_id
        }

        with Session(self.engine) as session:
            stmt = select(Article).where(Article.id == data["article_id"])
            article = session.execute(stmt).scalar_one_or_none()

            if article is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Article not found")
                return stub.DeleteArticleResponse(success=False)

            session.delete(article)
            session.commit()

            return stub.DeleteArticleResponse(success=True)

    # Assets Management
    def UploadAsset(self, request_iterator, context) -> stub.Asset:
        pass

    def UpdateAsset(self, request_iterator, context) -> stub.Asset:
        pass

    def DownloadAsset(self, request, context) -> stub.DownloadAssetResponse:
        pass

    def DeleteAsset(self, request, context) -> stub.DeleteAssetResponse:
        pass

    def ListAssets(self, request, context) -> stub.AssetList:
        pass


if __name__ == "__main__":
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    service.add_ArticleServiceServicer_to_server(ArticleService(), server)

    server_address = f"{Config.SERVICE_HOST}:{Config.SERVICE_PORT}"
    server.add_insecure_port(server_address)
    print(f"Starting gRPC server on {server_address}")
    server.start()
    try:
        server.wait_for_termination()
    except KeyboardInterrupt:
        print("Server is shutting down...")
        server.stop(0)
