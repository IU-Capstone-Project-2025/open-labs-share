# Import downloaded modules
import grpc
from sqlalchemy import create_engine, select, func
from sqlalchemy.orm import Session
import minio
from werkzeug.utils import secure_filename

# Import built-in modules
import os
import json
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
        
        policy = {
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Effect": "Allow",
                    "Principal": "*",
                    "Action": ["s3:GetObject"],
                    "Resource": ["arn:aws:s3:::articles/*"],
                },
            ],
        }
        self.minio_client.set_bucket_policy("articles", json.dumps(policy))

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
            total_count = session.execute(select(func.count(Article.id))).scalar()
            stmt = (select(Article)
                    .order_by(Article.created_at.desc())
                    .offset((request.page_number-1)*request.page_size)
                    .limit(request.page_size))
            articles = session.execute(stmt).scalars().all()

            alist = stub.ArticleList(total_count=total_count)
            for a in articles:
                alist.articles.append(stub.Article(**a.get_attrs()))
            return alist

    def GetArticlesByAuthorId(self, request, context) -> stub.ArticleList:
        data: dict = {
            "author_id": request.author_id,
            "page_number": request.page_number,
            "page_size": request.page_size
        }

        with Session(self.engine) as session:
            total_count_stmt = select(func.count(Article.id)).where(Article.owner_id == data["author_id"])
            total_count = session.execute(total_count_stmt).scalar()

            stmt = select(Article).where(Article.owner_id == data["author_id"]).order_by(Article.created_at.desc()).offset((data["page_number"] - 1) * data["page_size"]).limit(data["page_size"])
            articles = session.execute(stmt).scalars().all()

            article_list = stub.ArticleList(total_count=total_count)
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
        asset_metadata = next(request_iterator)

        if asset_metadata.HasField('metadata'):
            # The filename is now static and does not come from the client.
            static_filename = "article.pdf"
            asset: dict = {
                "article_id": asset_metadata.metadata.article_id,
                "filename": static_filename,
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

            # Define the temporary local path using a secure, static name
            temp_local_path = f'files/{new_asset.article_id}_{secure_filename(new_asset.filename)}'

            # Put file to MinIO bucket
            try:
                with open(temp_local_path, 'wb') as f:
                    for request in request_iterator:
                        if request.HasField('chunk'):
                            f.write(request.chunk)
                        else:
                            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                            context.set_details("Subsequent requests must contain chunk data")
                            return stub.Asset()

                # The object name in MinIO is now static.
                minio_object_name = f"{new_asset.article_id}/article.pdf"
                
                # Put the file in MinIO
                self.minio_client.fput_object(
                    "articles",
                    minio_object_name,
                    temp_local_path
                )

                # Clean up local file after upload
                os.remove(temp_local_path)

            except Exception as e:
                context.set_code(grpc.StatusCode.INTERNAL)
                context.set_details(f"Failed to upload asset to MinIO: {str(e)}")
                return stub.Asset()

            session.commit()
            session.refresh(new_asset)
            return stub.Asset(**new_asset.get_attrs())



    def UpdateAsset(self, request_iterator, context) -> stub.Asset:
        asset_metadata = next(request_iterator)

        if asset_metadata.HasField('metadata'):
            asset: dict = {
                "asset_id": asset_metadata.metadata.asset_id,
                "filename": secure_filename(asset_metadata.metadata.filename),
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
            session.refresh(article_asset)
            return stub.Asset(**article_asset.get_attrs())


    def DownloadAsset(self, request, context) -> stub.DownloadAssetResponse:
        def response_messages():
            # First, retrieve the asset from the database
            with Session(self.engine) as session:
                stmt = select(ArticleAsset).where(ArticleAsset.id == request.asset_id)
                asset = session.execute(stmt).scalar_one_or_none()

                if asset is None:
                    # https://grpc.github.io/grpc/python/grpc.html#grpc.ServicerContext.abort
                    # Unfortunately, we can't use context.abort because it raises an exception
                    # that can't be caught by the client.
                    context.set_code(grpc.StatusCode.NOT_FOUND)
                    context.set_details("Asset not found")
                    return

                # Download the file from MinIO
                try:
                    response = self.minio_client.get_object("articles", f"{asset.article_id}/{asset.filename}")
                    # Stream the file content in chunks
                    for chunk in response.stream(32 * 1024):
                        yield stub.DownloadAssetResponse(chunk=chunk)
                except Exception as e:
                    context.set_code(grpc.StatusCode.INTERNAL)
                    context.set_details(f"Failed to download asset from MinIO: {str(e)}")
                    return
                finally:
                    response.close()
                    response.release_conn()

        return response_messages()

    def DeleteAsset(self, request, context) -> stub.DeleteAssetResponse:
        data: dict = {
            "asset_id": request.asset_id
        }

        with Session(self.engine) as session:
            stmt = select(ArticleAsset).where(ArticleAsset.id == data["asset_id"])
            asset = session.execute(stmt).scalar_one_or_none()

            if asset is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Asset not found")
                return stub.DeleteAssetResponse(success=False)

            # Delete the file from MinIO
            try:
                self.minio_client.remove_object("articles", f"{asset.article_id}/{asset.filename}")
            except Exception as e:
                context.set_code(grpc.StatusCode.INTERNAL)
                context.set_details(f"Failed to delete asset from MinIO: {str(e)}")
                return stub.DeleteAssetResponse(success=False)

            session.delete(asset)
            session.commit()

            return stub.DeleteAssetResponse(success=True)

    def ListAssets(self, request, context) -> stub.AssetList:
        data: dict = {
            "article_id": request.article_id
        }

        with Session(self.engine) as session:
            stmt = select(ArticleAsset).where(ArticleAsset.article_id == data["article_id"])
            assets = session.execute(stmt).scalars().all()

            asset_list = stub.AssetList()
            for asset in assets:
                asset_list.assets.append(stub.Asset(**asset.get_attrs()))

            return asset_list

def serve():
    # Create a gRPC server
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))

    # Add the ArticleService to the server
    service.add_ArticleServiceServicer_to_server(ArticleService(), server)

    # Start the server
    port = Config.SERVICE_PORT
    server.add_insecure_port(f"[::]:{port}")
    server.start()
    print(f"Server started on port {port}")
    server.wait_for_termination()


if __name__ == "__main__":
    serve()
