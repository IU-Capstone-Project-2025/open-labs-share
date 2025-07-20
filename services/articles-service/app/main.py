# Import downloaded modules
import grpc
from sqlalchemy import create_engine, select, func
from sqlalchemy.orm import Session
import minio

# Import built-in modules
import sys
import os
import logging
import json
from concurrent import futures

# Fixes import path for proto files
sys.path.append(os.path.join(os.path.dirname(__file__), "proto"))

# Import project files
from config import Config
import proto.articles_service_pb2 as stub # Generated from articles_service.proto
import proto.articles_service_pb2_grpc as service # Generated from articles_service.proto
from grpc_health.v1 import health
from grpc_health.v1 import health_pb2
from grpc_health.v1 import health_pb2_grpc
from utils.models import Article, ArticleAsset

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

class ArticleService(service.ArticleServiceServicer):
    def __init__(self):
        self.logger = logging.getLogger(self.__class__.__name__)

        user = Config.POSTGRES_USER
        password = Config.POSTGRES_PASSWORD
        host = Config.POSTGRES_HOST
        port = Config.POSTGRES_PORT
        db_name = Config.POSTGRES_NAME
        url = f"postgresql://{user}:{password}@{host}:{port}/{db_name}"

        self.logger.info(f"Connecting to PostgreSQL at {url}")

        self.engine = create_engine(url, echo=False)

        from utils.models import Base
        Base.metadata.create_all(self.engine)

        self.logger.info(f"Connecting to MinIO at {Config.MINIO_ENDPOINT}")
        self.minio_client = minio.Minio(
            endpoint=Config.MINIO_ENDPOINT,
            access_key=Config.MINIO_ACCESS_KEY,
            secret_key=Config.MINIO_SECRET_KEY,
            secure=False
        )

        if not self.minio_client.bucket_exists("articles"): 
            self.logger.info(f"Creating bucket 'articles' at {Config.MINIO_ENDPOINT}")
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
            self.logger.info(f"Creating directory 'files'")
            os.makedirs('files')

    # Articles Management
    def CreateArticle(self, request, context):
        """
        Create a new article entry in the database.
        
        Args:
            request: CreateArticleRequest with fields:
                - owner_id (int): ID of the article owner
                - title (str): Article title
                - abstract (str): Article abstract/summary
            context: gRPC context
            
        Returns:
            stub.Article: The created article with generated ID and timestamps, or empty Article on error
        
        Errors:
            INVALID_ARGUMENT: If required fields are missing or invalid
        """

        self.logger.info(f"CreateArticle requested")

        data: dict = {
            "owner_id": request.owner_id,
            "title": request.title,
            "abstract": request.abstract
        }

        if data["owner_id"] is None or data["owner_id"] <= 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Owner ID is required, got '{data['owner_id']}'"
            context.set_details(error_message)
            
            self.logger.error(error_message)
            
            return stub.Article()
        
        if data["title"] is None or data["title"] == "":
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Title is required, got '{data['title']}'"
            context.set_details(error_message)
            
            self.logger.error(error_message)
            
            return stub.Article()

        if data["abstract"] is None or data["abstract"] == "":
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Abstract is required, got '{data['abstract']}'"
            context.set_details(error_message)
            
            self.logger.error(error_message)
            
            return stub.Article()

        with Session(self.engine) as session:
            article = Article(**data)
            session.add(article)
            session.commit()
            session.refresh(article)

            self.logger.info(f"Created article with ID {article.id}")

            return stub.Article(**article.get_attrs())

    def GetArticle(self, request, context) -> stub.Article:
        """
        Retrieve a specific article by ID.
        
        Args:
            request: GetArticleRequest with:
                - article_id (int): ID of the article to retrieve
            context: gRPC context
            
        Returns:
            stub.Article: Article data if found, empty Article if not found
            
        Errors:
            INVALID_ARGUMENT: If article_id is missing or invalid
            NOT_FOUND: If article doesn't exist
        """

        self.logger.info(f"GetArticle requested")

        data: dict = {
            "article_id": request.article_id
        }

        if data["article_id"] is None or data["article_id"] <= 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Article ID is required, got '{data['article_id']}'"
            context.set_details(error_message)

            self.logger.error(error_message)
            
            return stub.Article()

        with Session(self.engine) as session:
            stmt = select(Article).where(Article.id == data["article_id"])
            article = session.execute(stmt).scalar_one_or_none()

            if article is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                error_message = f"Article with ID '{data['article_id']}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

                return stub.Article()
            
            self.logger.info(f"Retrieved article with ID {article.id}")

            return stub.Article(**article.get_attrs())

    def GetArticles(self, request, context) -> stub.ArticleList:
        """
        Retrieve a paginated list of articles, optionally filtered by text.
        
        Args:
            request: GetArticlesRequest with:
                - page_number (int): Page number (1-based)
                - page_size (int): Number of articles per page
                - text (str, optional): Search text for title/abstract
                - tags_ids (list[int], optional): Tag IDs to filter by (not implemented)
            context: gRPC context
            
        Returns:
            stub.ArticleList: List of articles with total count, or empty list on error
            
        Note:
            TODO: Add specific filters (e.g. author_id, title, abstract, etc.)
        
        Errors:
            INVALID_ARGUMENT: If pagination parameters or text are invalid
        """

        self.logger.info(f"GetArticles requested")

        data: dict = {
            "page_number": request.page_number,
            "page_size": request.page_size,
            "text": request.text if request.HasField('text') else None,
            "tags_ids": request.tags_ids
        }

        # TODO: When tags for articles are implemented, add them to the query
        del data["tags_ids"]

        if data["page_number"] is None or data["page_number"] <= 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Page number must be greater than 0, got '{data['page_number']}'"
            context.set_details(error_message)

            self.logger.error(error_message)

            return stub.ArticleList()

        if data["page_size"] is None or data["page_size"] <= 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Page size must be greater than 0, got '{data['page_size']}'"
            context.set_details(error_message)

            self.logger.error(error_message)

            return stub.ArticleList()

        if data["text"] is not None and data["text"] == "":
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Text is required, got '{data['text']}'"
            context.set_details(error_message)

            self.logger.error(error_message)

            return stub.ArticleList()
        
        with Session(self.engine) as session:
            stmt = select(Article)

            if data["text"] is not None:
                stmt = stmt.where(Article.title.ilike(f"%{data['text']}%") | Article.abstract.ilike(f"%{data['text']}%"))
            
            stmt = stmt.offset((data["page_number"] - 1) * data["page_size"]).limit(data["page_size"])
            articles = session.execute(stmt).scalars().all()

            article_list = stub.ArticleList(total_count=len(articles))
            for article in articles:
                article_list.articles.append(stub.Article(**article.get_attrs()))

            self.logger.info(f"Retrieved {len(articles)} articles")

            return article_list

    def GetArticlesByUserId(self, request, context) -> stub.ArticleList:
        """
        Retrieve a paginated list of articles owned by a specific user.
        
        Args:
            request: GetArticlesByUserIdRequest with:
                - user_id (int): ID of the user to retrieve articles for
                - page_number (int): Page number (1-based)
                - page_size (int): Number of articles per page
            context: gRPC context
            
        Returns:
            stub.ArticleList: List of articles with total count, or empty list on error
            
        Errors:
            INVALID_ARGUMENT: If user_id, page_number, or page_size is invalid
        """

        self.logger.info(f"GetArticlesByUserId requested")

        data: dict = {
            "user_id": request.user_id,
            "page_number": request.page_number,
            "page_size": request.page_size
        }

        if data["user_id"] is None or data["user_id"] <= 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"User ID is required, got '{data['user_id']}'"
            context.set_details(error_message)

            self.logger.error(error_message)

            return stub.ArticleList()

        if data["page_number"] is None or data["page_number"] <= 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Page number must be greater than 0, got '{data['page_number']}'"
            context.set_details(error_message)

            self.logger.error(error_message)

            return stub.ArticleList()

        if data["page_size"] is None or data["page_size"] <= 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Page size must be greater than 0, got '{data['page_size']}'"
            context.set_details(error_message)

            self.logger.error(error_message)

            return stub.ArticleList()

        with Session(self.engine) as session:
            stmt = select(Article).where(Article.owner_id == data["user_id"]).offset((data["page_number"] - 1) * data["page_size"]).limit(data["page_size"])
            articles = session.execute(stmt).scalars().all()

            article_list = stub.ArticleList(total_count=len(articles))
            for article in articles:
                article_list.articles.append(stub.Article(**article.get_attrs()))

            self.logger.info(f"Retrieved {len(articles)} articles for user with ID {data['user_id']}")

            return article_list

    def UpdateArticle(self, request, context) -> stub.Article:
        """
        Update an existing article's details.
        
        Args:
            request: UpdateArticleRequest with:
                - article_id (int): ID of the article to update
                - title (str, optional): New article title
                - abstract (str, optional): New article abstract
            context: gRPC context
            
        Returns:
            stub.Article: Updated article data, or empty Article on error
            
        Errors:
            NOT_FOUND: If article doesn't exist
        """

        self.logger.info(f"UpdateArticle requested")

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

            self.logger.info(f"Updated article with ID {article.id}")

            return stub.Article(**article.get_attrs())

    def DeleteArticle(self, request, context) -> stub.DeleteArticleResponse:
        """
        Delete an article by ID.
        
        Args:
            request: DeleteArticleRequest with:
                - article_id (int): ID of the article to delete
            context: gRPC context
            
        Returns:
            stub.DeleteArticleResponse: Success status of the deletion
            
        Errors:
            NOT_FOUND: If article doesn't exist
        """

        self.logger.info(f"DeleteArticle requested")

        with Session(self.engine) as session:
            stmt = select(Article).where(Article.id == request.article_id)
            article = session.execute(stmt).scalar_one_or_none()

            if article is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Article not found")
                return stub.DeleteArticleResponse(success=False)

            session.delete(article)
            session.commit()

            self.logger.info(f"Deleted article with ID {article.id}")

            return stub.DeleteArticleResponse(success=True)

    def GetArticlesCount(self, request, context) -> stub.GetArticlesCountResponse:
        """
        Get the total number of articles in the database.
        
        Args:
            request: Empty GetArticlesCountRequest
            context: gRPC context
        
        Returns:
            stub.GetArticlesCountResponse: Total count of articles
        """

        self.logger.info(f"GetArticlesCount requested")

        with Session(self.engine) as session:
            stmt = select(func.count(Article.id))
            total_count = session.execute(stmt).scalar_one()

            self.logger.info(f"Total count: {total_count}")

            return stub.GetArticlesCountResponse(total_count=total_count)

    # Assets Management
    def UploadAsset(self, request_iterator, context) -> stub.Asset:
        """
        Upload a file asset for an article using streaming requests.
        
        Args:
            request_iterator: Stream of UploadAssetRequest messages:
                - First message: UploadAssetMetadata containing:
                    - article_id (int): ID of the article to attach asset to
                    - filename (str): Name of the file (ignored, uses static "article.pdf")
                    - filesize (int): Size of the file in bytes
                - Subsequent messages: File chunks as bytes
            context: gRPC context
            
        Returns:
            stub.Asset: Created asset with generated ID and upload timestamp, or empty Asset on error
            
        Errors:
            INVALID_ARGUMENT: If first message doesn't contain metadata or subsequent messages don't contain chunks
            NOT_FOUND: If article doesn't exist
            INTERNAL: If file upload fails
        """

        self.logger.info(f"UploadAsset requested")

        asset_metadata = next(request_iterator)

        if asset_metadata.HasField('metadata'):
            # The filename is now static and does not come from the client.
            static_filename = "article.pdf"
            asset: dict = {
                "article_id": asset_metadata.metadata.article_id,
                "filename": static_filename,
                "filesize": asset_metadata.metadata.filesize
            }

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

            # Define the temporary local path using a static name
            temp_local_path = f'files/{new_asset.article_id}_article.pdf'

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

            self.logger.info(f"Uploaded asset with ID {new_asset.id} for article with ID {new_asset.article_id}")

            return stub.Asset(**new_asset.get_attrs())

    def UpdateAsset(self, request_iterator, context) -> stub.Asset:
        """
        Update an existing asset file using streaming requests.
        
        Args:
            request_iterator: Stream of UpdateAssetRequest messages:
                - First message: UpdateAssetMetadata containing:
                    - asset_id (int): ID of the asset to update
                    - filename (str): New filename (ignored, uses static "article.pdf")
                    - filesize (int): New file size in bytes
                - Subsequent messages: New file chunks as bytes
            context: gRPC context
            
        Returns:
            stub.Asset: Updated asset data, or empty Asset on error
            
        Errors:
            INVALID_ARGUMENT: If first message doesn't contain metadata or subsequent messages don't contain chunks
            NOT_FOUND: If asset doesn't exist or MinIO deletion fails
            INTERNAL: If file upload fails
        """

        self.logger.info(f"UpdateAsset requested")

        asset_metadata = next(request_iterator)

        if asset_metadata.HasField('metadata'):
            static_filename = "article.pdf"
            asset: dict = {
                "asset_id": asset_metadata.metadata.asset_id,
                "filename": static_filename,
                "filesize": asset_metadata.metadata.filesize
            }
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

            # Define the temporary local path using a static name
            temp_local_path = f'files/{article_asset.article_id}_article.pdf'

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
                minio_object_name = f"{article_asset.article_id}/article.pdf"

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
            session.refresh(article_asset)

            self.logger.info(f"Updated asset with ID {article_asset.id} for article with ID {article_asset.article_id}")

            return stub.Asset(**article_asset.get_attrs())

    def DownloadAsset(self, request, context) -> stub.DownloadAssetResponse:
        """
        Download an asset file using streaming responses.
        
        Args:
            request: DownloadAssetRequest with:
                - asset_id (int): ID of the asset to download
            context: gRPC context
            
        Returns:
            Generator yielding DownloadAssetResponse messages:
                - First message: Asset metadata
                - Subsequent messages: File chunks as bytes
                
        Errors:
            NOT_FOUND: If asset doesn't exist
            INTERNAL: If file download fails
        """

        self.logger.info(f"DownloadAsset requested")

        def response_messages():
            # First, retrieve the asset from the database
            with Session(self.engine) as session:
                stmt = select(ArticleAsset).where(ArticleAsset.id == request.asset_id)
                asset = session.execute(stmt).scalar_one_or_none()

                if asset is None:
                    context.set_code(grpc.StatusCode.NOT_FOUND)
                    context.set_details("Asset not found")
                    return

                yield stub.DownloadAssetResponse(asset=stub.Asset(**asset.get_attrs()))

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

                self.logger.info(f"Downloaded asset with ID {asset.id} for article with ID {asset.article_id}")

        return response_messages()

    def DeleteAsset(self, request, context) -> stub.DeleteAssetResponse:
        """
        Delete an asset by ID.
        
        Args:
            request: DeleteAssetRequest with:
                - asset_id (int): ID of the asset to delete
            context: gRPC context
            
        Returns:
            stub.DeleteAssetResponse: Success status of the deletion
            
        Errors:
            NOT_FOUND: If asset doesn't exist
            INTERNAL: If file deletion from storage fails
        """

        self.logger.info(f"DeleteAsset requested")

        with Session(self.engine) as session:
            stmt = select(ArticleAsset).where(ArticleAsset.id == request.asset_id)
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

            self.logger.info(f"Deleted asset with ID {asset.id} for article with ID {asset.article_id}")

            return stub.DeleteAssetResponse(success=True)

    def ListAssets(self, request, context) -> stub.AssetList:
        """
        List all assets for a specific article.
        
        Args:
            request: ListAssetsRequest with:
                - article_id (int): ID of the article to list assets for
            context: gRPC context
            
        Returns:
            stub.AssetList: List of assets with total count
            
        Errors:
            NOT_FOUND: If article doesn't exist or has no assets
        """

        self.logger.info(f"ListAssets requested")

        with Session(self.engine) as session:
            stmt = select(ArticleAsset).where(ArticleAsset.article_id == request.article_id)
            assets = session.execute(stmt).scalars().all()

            asset_list = stub.AssetList()
            for asset in assets:
                asset_list.assets.append(stub.Asset(**asset.get_attrs()))

            self.logger.info(f"Listed {len(assets)} assets for article with ID {request.article_id}")

            return asset_list

def serve():
    # Create a gRPC server
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))

    # Add ArticleService to the server
    service.add_ArticleServiceServicer_to_server(ArticleService(), server)

    # Add HealthServicer to the server
    health_servicer = health.HealthServicer()
    health_pb2_grpc.add_HealthServicer_to_server(health_servicer, server)
    health_servicer.set("articles.ArticleService", health_pb2.HealthCheckResponse.SERVING)

    # Start the server
    server.add_insecure_port(f"[::]:{Config.SERVICE_PORT}")
    server.start()
    logging.info(f"Server started on port {Config.SERVICE_PORT}")
    server.wait_for_termination()

if __name__ == '__main__':
    serve()
