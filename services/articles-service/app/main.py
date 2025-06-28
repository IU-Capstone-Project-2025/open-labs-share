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
import proto.articles_service_pb2 as stub # Generated from articles_service.proto
import proto.articles_service_pb2_grpc as service # Generated from articles_service.proto
from utils.models import Article, ArticleAsset


class ArticleService(service.ArticleServiceServicer):
    def __init__(self):
        user = Config.DB_USER
        password = Config.DB_PASSWORD
        host = Config.DB_HOST
        port = Config.DB_PORT
        db_name = Config.DB_NAME
        url = f"postgresql://{user}:{password}@{host}:{port}/{db_name}"

        print(f"Creating connection at {url}")

        self.engine = create_engine(url, echo=True)

        from utils.models import Base
        Base.metadata.create_all(self.engine)

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
            "title": request.title.encode('utf-8').decode('utf-8'),
            "abstract": request.abstract.encode('utf-8').decode('utf-8')
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
            "title": request.title.encode('utf-8').decode('utf-8') if request.HasField("title") else None,
            "abstract": request.abstract.encode('utf-8').decode('utf-8') if request.HasField("abstract") else None
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
        asset_metadata = next(request_iterator)

        if asset_metadata.HasField('metadata'):
            asset: dict = {
                "article_id": asset_metadata.metadata.article_id,
                "filename": asset_metadata.metadata.filename.encode('utf-8').decode('utf-8'),
                "filesize": asset_metadata.metadata.filesize
            }
            print(f'{asset=}')
        else:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details("First request must contain metadata")
            return stub.Asset()

        with Session(self.engine) as session:
            stmt = select(Article).where(Article.id == asset["article_id"])
            article = session.execute(stmt).scalar_one_or_none()

            if article is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Article not found")
                return stub.Asset()

            # Create a new ArticleAsset instance
            new_asset = ArticleAsset(**asset)
            session.add(new_asset)

            # Put file to MinIO bucket
            try:
                with open(f'files/{new_asset.filename}', 'wb') as f:
                    for request in request_iterator:
                        if request.HasField('chunk'):
                            f.write(request.chunk)
                        else:
                            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                            context.set_details("Subsequent requests must contain chunk data")
                            return stub.Asset()

                # Put the file in MinIO
                self.minio_client.fput_object(
                    "articles",
                    f"{new_asset.article_id}/{new_asset.filename}",
                    f'files/{new_asset.filename}'
                )

                # Clean up local file after upload
                os.remove(f'files/{new_asset.filename}'"")

            except Exception as e:
                context.set_code(grpc.StatusCode.INTERNAL)
                context.set_details(f"Failed to upload asset to MinIO: {str(e)}")
                return stub.Asset()

            session.commit()
            return stub.Asset(**new_asset.get_attrs())



    def UpdateAsset(self, request_iterator, context) -> stub.Asset:
        asset_metadata = next(request_iterator)

        if asset_metadata.HasField('metadata'):
            asset: dict = {
                "asset_id": asset_metadata.metadata.asset_id,
                "filename": asset_metadata.metadata.filename.encode('utf-8').decode('utf-8'),
                "filesize": asset_metadata.metadata.filesize
            }
            print(f'{asset=}')
        else:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details("First request must contain metadata")
            return stub.Asset()

        with Session(self.engine) as session:
            stmt = select(ArticleAsset).where(ArticleAsset.id == asset["asset_id"])
            article_asset = session.execute(stmt).scalar_one_or_none()

            if article_asset is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Asset not found")
                return stub.Asset()

            # Try to remove the old asset file from MinIO
            try:
                self.minio_client.remove_object('articles', f"{article_asset.article_id}/{article_asset.filename}")
            except Exception as e:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details(f"Failed to delete asset from MinIO: {str(e)}")
                return stub.Asset()

            # Update the asset's filename and filesize
            article_asset.filename = asset["filename"]
            article_asset.filesize = asset["filesize"]

            # Put file to MinIO bucket
            try:
                with open(f'files/{article_asset.filename}', 'wb') as f:
                    for request in request_iterator:
                        if request.HasField('chunk'):
                            f.write(request.chunk)
                        else:
                            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                            context.set_details("Subsequent requests must contain chunk data")
                            return stub.Asset()

                # Put the file in MinIO
                self.minio_client.fput_object(
                    "articles",
                    f"{article_asset.article_id}/{article_asset.filename}",
                    f'files/{article_asset.filename}'
                )

                # Clean up local file after upload
                os.remove(f'files/{article_asset.filename}')

            except Exception as e:
                context.set_code(grpc.StatusCode.INTERNAL)
                context.set_details(f"Failed to upload asset to MinIO: {str(e)}")
                return stub.Asset()

            session.commit()
            return stub.Asset(**article_asset.get_attrs())

    def DownloadAsset(self, request, context) -> stub.DownloadAssetResponse:
        def response_messages():
            data: dict = {
                "asset_id": request.asset_id
            }

            with Session(self.engine) as session:
                stmt = select(ArticleAsset).where(ArticleAsset.id == data["asset_id"])
                article_asset = session.execute(stmt).scalar_one_or_none()

                if article_asset is None:
                    context.set_code(grpc.StatusCode.NOT_FOUND)
                    context.set_details("Asset not found")
                    return stub.DownloadAssetResponse()

                # Send asset metadata
                yield stub.DownloadAssetResponse(asset=stub.Asset(**article_asset.get_attrs()))

                # Download the file from MinIO
                try:
                    self.minio_client.fget_object(
                        "articles",
                        f"{article_asset.article_id}/{article_asset.filename}",
                        f'files/{article_asset.filename}'
                    )
                except Exception as e:
                    context.set_code(grpc.StatusCode.INTERNAL)
                    context.set_details(f"Failed to download asset from MinIO: {str(e)}")
                    return stub.DownloadAssetResponse()

                with open(f'files/{article_asset.filename}', 'rb') as f:
                    while True:
                        chunk = f.read(8 * 1024)

                        if not chunk:
                            break

                        yield stub.DownloadAssetResponse(chunk=chunk)

                # Clean up local file after download
                os.remove(f'files/{article_asset.filename}')

        return response_messages()

    def DeleteAsset(self, request, context) -> stub.DeleteAssetResponse:
        data: dict = {
            "asset_id": request.asset_id
        }

        print(data)

        with Session(self.engine) as session:
            stmt = select(ArticleAsset).where(ArticleAsset.id == data["asset_id"])
            asset = session.execute(stmt).scalar_one_or_none()

            if asset is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Asset not found")
                return stub.DeleteAssetResponse(success=False)

            session.delete(asset)

            # Try to remove the asset file from MinIO
            try:
                self.minio_client.remove_object('articles', f"{asset.article_id}/{asset.filename}")
            except Exception as e:
                context.set_code(grpc.StatusCode.INTERNAL)
                context.set_details(f"Failed to delete asset from MinIO: {str(e)}")
                return stub.DeleteAssetResponse(success=False)

            session.commit()

            return stub.DeleteAssetResponse(success=True)

    def ListAssets(self, request, context) -> stub.AssetList:
        data: dict = {
            "article_id": request.article_id
        }
        with Session(self.engine) as session:
            stmt = select(Article).where(Article.id == data["article_id"])
            article = session.execute(stmt).scalar_one_or_none()

            if article is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Article not found")
                return stub.AssetList()

            if article.assets is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("No assets found for this article")
                return stub.AssetList()

            asset_list = stub.AssetList()
            asset_list.total_count = len(article.assets)
            asset_list.assets.extend([stub.Asset(**asset.get_attrs()) for asset in article.assets])

            return asset_list

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
