# Import downloaded modules
import grpc
import minio
from pymongo import MongoClient
from sqlalchemy.orm import Session
from sqlalchemy import create_engine, select
from google.protobuf.timestamp_pb2 import Timestamp

# Import built-in modules
import sys
import os
import logging
from concurrent import futures

# Fixes import path for proto files
sys.path.append(os.path.join(os.path.dirname(__file__), "proto"))

# Import project files
from config import Config
from utils.models import Lab, LabAsset, ArticleRelation, Submission, SubmissionAsset
import proto.labs_service_pb2 as labs_stub # Generated from labs.proto
import proto.labs_service_pb2_grpc as labs_service # Generated from labs.proto
import proto.submissions_service_pb2 as submissions_stub  # Generated from submissions_service.proto
import proto.submissions_service_pb2_grpc as submissions_service  # Generated from submissions_service.proto


logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

class Tools:
    _minio_client: minio.Minio = None
    _mongo_client: MongoClient = None
    _postgresql_engine = None

    def __init__(self):
        self.logger = logging.getLogger(self.__class__.__name__)

    def get_minio_client(self) -> minio.Minio:
        if self._minio_client is None:
            self.logger.info(f"Connection to MinIO at {Config.MINIO_ENDPOINT}")
            self._minio_client = minio.Minio(
                endpoint=Config.MINIO_ENDPOINT,
                access_key=Config.MINIO_ACCESS_KEY,
                secret_key=Config.MINIO_SECRET_KEY,
                secure=False
            )
        return self._minio_client

    def get_mongo_client(self) -> MongoClient:
        if self._mongo_client is None:
            # Include authentication database in connection string
            url = f"mongodb://{Config.MONGODB_USER}:{Config.MONGODB_PASSWORD}@{Config.MONGODB_HOST}:{Config.MONGODB_PORT}/{Config.MONGODB_NAME}?authSource=admin"
            
            self.logger.info(f"Connecting to MongoDB at {url}")
            self._mongo_client = MongoClient(url)
        return self._mongo_client

    def get_postgresql_engine(self):
        if self._postgresql_engine is None:
            user = Config.POSTGRESQL_USER
            password = Config.POSTGRESQL_PASSWORD
            host = Config.POSTGRESQL_HOST
            port = Config.POSTGRESQL_PORT
            db_name = Config.POSTGRESQL_NAME
            url = f"postgresql://{user}:{password}@{host}:{port}/{db_name}"

            self.logger.info(f"Connecting to PostgreSQL at {url}")
            self._postgresql_engine = create_engine(url, echo=False)

            # Create tables if they don't exist
            from utils.models import Base
            Base.metadata.create_all(self._postgresql_engine)

        return self._postgresql_engine


tools = Tools()


class LabService(labs_service.LabServiceServicer):
    def __init__(self):
        self.engine = tools.get_postgresql_engine()
        self.minio_client = tools.get_minio_client()
        self.logger = logging.getLogger(self.__class__.__name__)

        if not self.minio_client.bucket_exists("labs"):
            self.minio_client.make_bucket("labs")

        # Ensure the temporary files directory exists
        if not os.path.exists('files'):
            os.makedirs('files')

    # ------- Labs Management -------
    def CreateLab(self, request, context) -> labs_stub.Lab:
        """
        Create a new lab.
        
        Args:
            request: CreateLabRequest containing:
                - owner_id (int): ID of the lab owner
                - title (str): Lab title
                - abstract (str): Lab abstract/summary
                - related_articles (ArticleList, optional): List of related article IDs
            context: gRPC context
            
        Returns:
            labs_stub.Lab: Created lab with generated ID and timestamps
            
        Raises:
            grpc.StatusCode.INVALID_ARGUMENT: If title or abstract is empty or None
        """

        self.logger.info(f"CreateLab requested")

        data: dict = {
            "owner_id": request.owner_id,
            "title": request.title,
            "abstract": request.abstract,
        }

        if data["title"] is None or data["title"] == "":
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(f"Title is required, got '{data['title']}'")
            return labs_stub.Lab()
        
        if data["abstract"] is None or data["abstract"] == "":
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(f"Abstract is required, got '{data['abstract']}'")
            return labs_stub.Lab()

        with Session(self.engine) as session:
            new_lab = Lab(**data)
            session.add(new_lab)

            if request.HasField("related_articles"):
                article_ids = request.related_articles.article_id
                new_lab.articles.extend(article_ids)

            session.commit()

            self.logger.info(f"Created Lab with id={new_lab.id}, title={new_lab.title}")

            return labs_stub.Lab(**new_lab.get_attrs())


    def GetLab(self, request, context) -> labs_stub.Lab:
        """
        Retrieve a specific lab by ID.
        
        Args:
            request: GetLabRequest containing:
                - lab_id (int): ID of the lab to retrieve
            context: gRPC context
            
        Returns:
            labs_stub.Lab: Lab data if found, empty Lab if not found
            
        Raises:
            grpc.StatusCode.NOT_FOUND: If lab doesn't exist
        """

        self.logger.info(f"GetLab requested")

        data: dict = {
            "lab_id": request.lab_id
        }

        with Session(self.engine) as session:
            stmt = select(Lab).where(Lab.id == data["lab_id"])
            lab = session.execute(stmt).scalar_one_or_none()

            if lab is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Lab not found")
                return labs_stub.Lab()

            self.logger.info(f"Retrieved Lab with id={lab.id}, title={lab.title}")

            return labs_stub.Lab(**lab.get_attrs())


    def GetLabs(self, request, context) -> labs_stub.LabList:
        """
        Retrieve a paginated list of labs.
        
        Args:
            request: GetLabsRequest containing:
                - page_number (int): Page number (1-based)
                - page_size (int): Number of labs per page
            context: gRPC context
            
        Returns:
            labs_stub.LabList: List of labs with total count
            
        Raises:
            grpc.StatusCode.INVALID_ARGUMENT: If page_number or page_size is None or <= 0
        """

        self.logger.info(f"GetLabs requested")

        data: dict = {
            "page_number": request.page_number,
            "page_size": request.page_size
        }

        if data["page_number"] is None or data["page_number"] <= 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(f"Page number is required, got '{data['page_number']}'")
            return labs_stub.LabList()
        
        if data["page_size"] is None or data["page_size"] <= 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(f"Page size is required, got '{data['page_size']}'")
            return labs_stub.LabList()

        with Session(self.engine) as session:
            # Get total count of labs
            total_count = session.query(Lab).count()

            # Get paginated labs
            stmt = select(Lab).offset((data["page_number"] - 1) * data["page_size"]).limit(data["page_size"])
            labs = session.execute(stmt).scalars().all()

            lab_list = labs_stub.LabList(total_count=total_count)
            for lab in labs:
                lab_list.labs.append(labs_stub.Lab(**lab.get_attrs()))

            self.logger.info(f"Retrieved {len(labs)} labs, page {data['page_number']} of size {data['page_size']}")

            return lab_list


    def UpdateLab(self, request, context) -> labs_stub.Lab:
        """
        Update an existing lab.
        
        Args:
            request: UpdateLabRequest containing:
                - lab_id (int): ID of the lab to update
                - title (str, optional): New lab title
                - abstract (str, optional): New lab abstract
                - related_articles (ArticleList, optional): New list of related article IDs
            context: gRPC context
            
        Returns:
            labs_stub.Lab: Updated lab data
            
        Raises:
            grpc.StatusCode.NOT_FOUND: If lab doesn't exist
        """

        self.logger.info(f"UpdateLab requested")

        data: dict = {
            "lab_id": request.lab_id,
            "title": request.title if request.HasField("title") else None,
            "abstract": request.abstract if request.HasField("abstract") else None,
            "related_articles": request.related_articles if request.HasField("related_articles") else None
        }

        with Session(self.engine) as session:
            stmt = select(Lab).where(Lab.id == data["lab_id"])
            lab = session.execute(stmt).scalar_one_or_none()

            if lab is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Lab not found")
                return labs_stub.Lab()

            if data["title"] is not None:
                lab.title = data["title"]

            if data["abstract"] is not None:
                lab.abstract = data["abstract"]

            if data["related_articles"] is not None:
                article_ids = data["related_articles"].article_id
                lab.articles.clear()
                lab.articles.extend(article_ids)

            session.commit()

            self.logger.info(f"Updated Lab with id={lab.id}, title={lab.title}")

            return labs_stub.Lab(**lab.get_attrs())


    def DeleteLab(self, request, context) -> labs_stub.DeleteLabResponse:
        """
        Delete a lab by ID.
        
        Args:
            request: DeleteLabRequest containing:
                - lab_id (int): ID of the lab to delete
            context: gRPC context
            
        Returns:
            labs_stub.DeleteLabResponse: Success status of the deletion
            
        Raises:
            grpc.StatusCode.NOT_FOUND: If lab doesn't exist
            grpc.StatusCode.INTERNAL: If asset deletion from storage fails
        """

        self.logger.info(f"DeleteLab requested")

        data: dict = {
            "lab_id": request.lab_id
        }

        with Session(self.engine) as session:
            stmt = select(Lab).where(Lab.id == data["lab_id"])
            lab = session.execute(stmt).scalar_one_or_none()

            if lab is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Lab not found")
                return labs_stub.DeleteLabResponse(success=False)

            session.delete(lab)
            session.commit()

            # Remove all assets from MinIO
            try:
                for asset in lab.assets:
                    self.minio_client.remove_object('labs', f"{lab.id}/{asset.filename}")
            except Exception as e:
                context.set_code(grpc.StatusCode.INTERNAL)
                context.set_details(f"Failed to delete assets from MinIO: {str(e)}")
                return labs_stub.DeleteLabResponse(success=False)

            self.logger.info(f"Deleted Lab with id={lab.id}, title={lab.title}")

            return labs_stub.DeleteLabResponse(success=True)

    # ------- Lab Assets Management -------
    def UploadAsset(self, request_iterator, context) -> labs_stub.Asset:
        """
        Upload a file asset for a lab using streaming.
        
        Args:
            request_iterator: Stream of UploadAssetRequest messages:
                - First message: UploadAssetMetadata containing:
                    - lab_id (int): ID of the lab to attach asset to
                    - filename (str): Name of the file
                    - filesize (int): Size of the file in bytes
                - Subsequent messages: File chunks as bytes
            context: gRPC context
            
        Returns:
            labs_stub.Asset: Created asset with generated ID and upload timestamp
            
        Raises:
            grpc.StatusCode.INVALID_ARGUMENT: If first message doesn't contain metadata, or if filename is empty/None, or if filesize is None or <= 0
            grpc.StatusCode.NOT_FOUND: If lab doesn't exist
            grpc.StatusCode.INTERNAL: If file upload fails
        """

        self.logger.info(f"UploadAsset requested")

        # Check for metadata being the first request
        metadata_request = next(request_iterator)

        if metadata_request.HasField('metadata'):
            data: dict = {
                "lab_id": metadata_request.metadata.lab_id,
                "filename": metadata_request.metadata.filename,
                "filesize": metadata_request.metadata.filesize
            }

            if data["filename"] is None or data["filename"] == "":
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details(f"Filename is required, got '{data['filename']}'")
                return labs_stub.Asset()

            if data["filesize"] is None or data["filesize"] <= 0:
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details(f"Filesize is required, got '{data['filesize']}'")
                return labs_stub.Asset()

        else:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details("First request must contain metadata")
            return labs_stub.Asset()

        with Session(self.engine) as session:
            # Check if lab exists
            stmt = select(Lab).where(Lab.id == data["lab_id"])
            lab = session.execute(stmt).scalar_one_or_none()

            if lab is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Lab not found")
                return labs_stub.Asset()

            # Create new asset
            new_asset = LabAsset(**data)
            session.add(new_asset)

            # Put file in minio bucket
            try:
                # Download the file to a local path
                with open(f'files/{new_asset.filename}', 'wb') as f:
                    for request in request_iterator:
                        if request.HasField('chunk'):
                            f.write(request.chunk)
                        else:
                            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                            context.set_details("Subsequent requests must contain chunk data")
                            return labs_stub.Asset()

                # Put the file in MinIO
                self.minio_client.fput_object(
                    "labs",
                    f"{new_asset.lab_id}/{new_asset.filename}",
                    f'files/{new_asset.filename}'
                )

                # Clean up local file after upload
                os.remove(f'files/{new_asset.filename}'"")

            except Exception as e:
                context.set_code(grpc.StatusCode.INTERNAL)
                context.set_details(f"Failed to upload asset to MinIO: {str(e)}")
                return labs_stub.Asset()

            session.commit()

            self.logger.info(f"Uploaded asset with id={new_asset.id}, filename={new_asset.filename} for lab_id={new_asset.lab_id}")

            return labs_stub.Asset(**new_asset.get_attrs())


    def UpdateAsset(self, request_iterator, context) -> labs_stub.Asset:
        """
        Update an existing lab asset file using streaming.
        
        Args:
            request_iterator: Stream of UpdateAssetRequest messages:
                - First message: UpdateAssetMetadata containing:
                    - asset_id (int): ID of the asset to update
                    - filename (str): New filename
                    - filesize (int): New file size in bytes
                - Subsequent messages: New file chunks as bytes
            context: gRPC context
            
        Returns:
            labs_stub.Asset: Updated asset data
            
        Raises:
            grpc.StatusCode.INVALID_ARGUMENT: If first message doesn't contain metadata, or if asset_id is None, or if filename is empty/None, or if filesize is None or <= 0
            grpc.StatusCode.NOT_FOUND: If asset doesn't exist
            grpc.StatusCode.INTERNAL: If file upload fails
        """

        self.logger.info(f"UpdateAsset requested")

        # Check for metadata being the first request
        metadata_request = next(request_iterator)

        if metadata_request.HasField('metadata'):
            data: dict = {
                "asset_id": metadata_request.metadata.asset_id,
                "filename": metadata_request.metadata.filename,
                "filesize": metadata_request.metadata.filesize
            }

            if data["asset_id"] is None:
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details(f"Asset ID is required, got '{data['asset_id']}'")
                return labs_stub.Asset()

            if data["filename"] is None or data["filename"] == "":
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details(f"Filename is required, got '{data['filename']}'")
                return labs_stub.Asset()

            if data["filesize"] is None or data["filesize"] <= 0:
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details(f"Filesize is required, got '{data['filesize']}'")
                return labs_stub.Asset()

        else:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details("First request must contain metadata")
            return labs_stub.Asset()

        with Session(self.engine) as session:
            # Check if lab exists
            stmt = select(LabAsset).where(LabAsset.id == data["asset_id"])
            lab_asset = session.execute(stmt).scalar_one_or_none()

            if lab_asset is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Asset not found")
                return labs_stub.Asset()

            # Try to remove the old asset file from MinIO
            try:
                self.minio_client.remove_object('labs', f"{lab_asset.lab_id}/{lab_asset.filename}")
            except Exception as e:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details(f"Failed to delete asset from MinIO: {str(e)}")
                return labs_stub.Asset()

            lab_asset.filename = data["filename"]
            lab_asset.filesize = data["filesize"]


            with open(f'files/{lab_asset.filename}', 'wb') as f:
                for request in request_iterator:
                    if request.HasField('chunk'):
                        f.write(request.chunk)
                    else:
                        context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                        context.set_details("Subsequent requests must contain chunk data")
                        return labs_stub.Asset()

            # Put the file in MinIO
            self.minio_client.fput_object(
                "labs",
                f"{lab_asset.lab_id}/{lab_asset.filename}",
                f'files/{lab_asset.filename}'
            )

            # Clean up local file after upload
            os.remove(f'files/{lab_asset.filename}')

            session.commit()

            self.logger.info(f"Updated asset with id={lab_asset.id}, filename={lab_asset.filename} for lab_id={lab_asset.lab_id}")

            return labs_stub.Asset(**lab_asset.get_attrs())


    def DownloadAsset(self, request, context) -> labs_stub.DownloadAssetResponse:
        """
        Download a lab asset file using streaming.
        
        Args:
            request: DownloadAssetRequest containing:
                - asset_id (int): ID of the asset to download
            context: gRPC context
            
        Returns:
            Generator yielding DownloadAssetResponse messages:
                - First message: Asset metadata
                - Subsequent messages: File chunks as bytes
                
        Raises:
            grpc.StatusCode.NOT_FOUND: If asset doesn't exist
            grpc.StatusCode.INTERNAL: If file download fails
        """

        self.logger.info(f"DownloadAsset requested")

        def response_messages():
            data: dict = {
                "asset_id": request.asset_id
            }

            with Session(self.engine) as session:
                stmt = select(LabAsset).where(LabAsset.id == data["asset_id"])
                lab_asset = session.execute(stmt).scalar_one_or_none()

                if lab_asset is None:
                    context.set_code(grpc.StatusCode.NOT_FOUND)
                    context.set_details("Asset not found")
                    return labs_stub.DownloadAssetResponse()

                yield labs_stub.DownloadAssetResponse(asset=labs_stub.Asset(**lab_asset.get_attrs()))

                # Download the file from MinIO
                try:
                    self.minio_client.fget_object(
                        "labs",
                        f"{lab_asset.lab_id}/{lab_asset.filename}",
                        f'files/{lab_asset.filename}'
                    )
                except Exception as e:
                    context.set_code(grpc.StatusCode.INTERNAL)
                    context.set_details(f"Failed to download asset from MinIO: {str(e)}")
                    return labs_stub.DownloadAssetResponse()

                with open(f'files/{lab_asset.filename}', 'rb') as f:
                    while True:
                        chunk = f.read(8 * 1024)

                        if not chunk:
                            break

                        yield labs_stub.DownloadAssetResponse(chunk=chunk)

                # Clean up local file after download
                os.remove(f'files/{lab_asset.filename}')

        self.logger.info(f"Downloading asset with id={request.asset_id}")

        return response_messages()


    def DeleteAsset(self, request, context) -> labs_stub.DeleteAssetResponse:
        """
        Delete a lab asset by ID.
        
        Args:
            request: DeleteAssetRequest containing:
                - asset_id (int): ID of the asset to delete
            context: gRPC context
            
        Returns:
            labs_stub.DeleteAssetResponse: Success status of the deletion
            
        Raises:
            grpc.StatusCode.NOT_FOUND: If asset doesn't exist
            grpc.StatusCode.INTERNAL: If file deletion from storage fails
        """

        self.logger.info(f"DeleteAsset requested")

        data: dict = {
            "asset_id": request.asset_id
        }

        with Session(self.engine) as session:
            stmt = select(LabAsset).where(LabAsset.id == data["asset_id"])
            asset = session.execute(stmt).scalar_one_or_none()

            if asset is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Asset not found")
                return labs_stub.DeleteAssetResponse(success=False)

            session.delete(asset)

            # Remove the asset from MinIO
            try:
                self.minio_client.remove_object('labs', f"{asset.lab_id}/{asset.filename}")
            except Exception as e:
                context.set_code(grpc.StatusCode.INTERNAL)
                context.set_details(f"Failed to delete asset from MinIO: {str(e)}")
                return labs_stub.DeleteAssetResponse(success=False)

            session.commit()

            self.logger.info(f"Deleted asset with id={asset.id}, filename={asset.filename} for lab_id={asset.lab_id}")

            return labs_stub.DeleteAssetResponse(success=True)


    def ListAssets(self, request, context) -> labs_stub.AssetList:
        """
        List all assets for a specific lab.
        
        Args:
            request: ListAssetsRequest containing:
                - lab_id (int): ID of the lab to list assets for
            context: gRPC context
            
        Returns:
            labs_stub.AssetList: List of assets with total count
            
        Raises:
            grpc.StatusCode.NOT_FOUND: If lab doesn't exist or has no assets
        """

        self.logger.info(f"ListAssets requested")

        data: dict = {
            "lab_id": request.lab_id
        }

        with Session(self.engine) as session:
            stmt = select(Lab).where(Lab.id == data["lab_id"])
            lab = session.execute(stmt).scalar_one_or_none()

            if lab is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Lab not found")
                return labs_stub.AssetList()

            if not lab.assets:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Lab has no assets")
                return labs_stub.AssetList()

            asset_list = labs_stub.AssetList()
            asset_list.total_count = len(lab.assets)
            asset_list.assets.extend([labs_stub.Asset(**asset.get_attrs()) for asset in lab.assets])

            self.logger.info(f"Listed {len(lab.assets)} assets for lab_id={data['lab_id']}")

            return asset_list


class SubmissionService(submissions_service.SubmissionServiceServicer):
    def __init__(self):
        self.minio_client = tools.get_minio_client()
        self.postgresql_engine = tools.get_postgresql_engine()

        mongo_client = tools.get_mongo_client()
        self.submissions_texts = mongo_client[Config.MONGODB_NAME]["submissions_texts"]

        self.logger = logging.getLogger(self.__class__.__name__)

        self.get_grpc_status: dict = {
            0: submissions_stub.Status.NOT_GRADED,
            1: submissions_stub.Status.IN_PROGRESS,
            2: submissions_stub.Status.ACCEPTED,
            3: submissions_stub.Status.REJECTED
        }

        self.get_db_status: dict = {
            submissions_stub.Status.NOT_GRADED: 0,
            submissions_stub.Status.IN_PROGRESS: 1,
            submissions_stub.Status.ACCEPTED: 2,
            submissions_stub.Status.REJECTED: 3
        }

        if not self.minio_client.bucket_exists("submissions"):
            self.minio_client.make_bucket("submissions")

        # Ensure the temporary files directory exists
        if not os.path.exists('files'):
            os.makedirs('files')

    # Submissions Management
    def CreateSubmission(self, request, context) -> submissions_stub.Submission:
        """
        Create a new submission for a lab.
        
        Args:
            request: CreateSubmissionRequest containing:
                - lab_id (int): ID of the lab to submit to
                - owner_id (int): ID of the submission owner
                - text (str): Submission text content
            context: gRPC context
            
        Returns:
            submissions_stub.Submission: Created submission with generated ID and timestamps
            
        Raises:
            grpc.StatusCode.NOT_FOUND: If lab doesn't exist
        """

        self.logger.info(f"CreateSubmission requested")

        data: dict = {
            "lab_id": request.lab_id,
            "owner_id": request.owner_id,
        }

        text = request.text

        with Session(self.postgresql_engine) as session:
            # Check if lab exists
            stmt = select(Lab).where(Lab.id == data["lab_id"])
            lab = session.execute(stmt).scalar_one_or_none()

            if lab is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Lab not found")
                return submissions_stub.Submission()

            # Create new submission
            new_submission = Submission(**data)
            session.add(new_submission)

            session.commit()

            self.submissions_texts.insert_one({
                "submission_id": str(new_submission.id),
                "text": text
            })

            result = submissions_stub.Submission(**new_submission.get_attrs())
            result.text = text
            result.status = self.get_grpc_status[result.status]

            self.logger.info(f"Created Submission with id={new_submission.id}, lab_id={new_submission.lab_id}")

            return result

    def GetSubmission(self, request, context) -> submissions_stub.Submission:
        """
        Retrieve a specific submission by ID.
        
        Args:
            request: GetSubmissionRequest containing:
                - submission_id (int): ID of the submission to retrieve
            context: gRPC context
            
        Returns:
            submissions_stub.Submission: Submission data if found, empty Submission if not found
            
        Raises:
            grpc.StatusCode.NOT_FOUND: If submission doesn't exist
        """

        self.logger.info(f"GetSubmission requested")

        data: dict = {
            "submission_id": request.submission_id
        }

        with Session(self.postgresql_engine) as session:
            stmt = select(Submission).where(Submission.id == data["submission_id"])
            submission = session.execute(stmt).scalar_one_or_none()

            if submission is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Submission not found")
                return submissions_stub.Submission()

            # Fetch text from MongoDB
            text_data = self.submissions_texts.find_one({"submission_id": str(submission.id)})

            result = submissions_stub.Submission(**submission.get_attrs())
            result.status = self.get_grpc_status[result.status]
            if text_data:
                result.text = text_data.get("text", "")

            self.logger.info(f"Retrieved Submission with id={submission.id}, lab_id={submission.lab_id}")

            return result

    def GetSubmissions(self, request, context) -> submissions_stub.SubmissionList:
        """
        Retrieve a paginated list of submissions for a specific lab.
        
        Args:
            request: GetSubmissionsRequest containing:
                - lab_id (int): ID of the lab to get submissions for
                - page_number (int): Page number (1-based)
                - page_size (int): Number of submissions per page
            context: gRPC context
            
        Returns:
            submissions_stub.SubmissionList: List of submissions with total count
            
        Raises:
            grpc.StatusCode.INVALID_ARGUMENT: If page_number or page_size is None or <= 0
            grpc.StatusCode.NOT_FOUND: If lab doesn't exist
        """

        self.logger.info(f"GetSubmissions requested")

        data: dict = {
            "lab_id": request.lab_id,
            "page_number": request.page_number,
            "page_size": request.page_size
        }

        if data["page_number"] is None or data["page_number"] <= 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(f"Page number is required, got '{data['page_number']}'")
            return submissions_stub.SubmissionList()

        if data["page_size"] is None or data["page_size"] <= 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(f"Page size is required, got '{data['page_size']}'")
            return submissions_stub.SubmissionList()

        with Session(self.postgresql_engine) as session:
            # Check if lab exists
            stmt = select(Lab).where(Lab.id == data["lab_id"])
            lab = session.execute(stmt).scalar_one_or_none()

            if lab is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Lab not found")
                return submissions_stub.SubmissionList()

            # Get paginated submissions
            stmt = select(Submission).where(Submission.lab_id == data["lab_id"]).offset((data["page_number"] - 1) * data["page_size"]).limit(data["page_size"])
            submissions = session.execute(stmt).scalars().all()

            submission_list = submissions_stub.SubmissionList(total_count=len(submissions))
            for submission in submissions:
                # Fetch text from MongoDB
                text_data = self.submissions_texts.find_one({"submission_id": str(submission.id)})
                result = submissions_stub.Submission(**submission.get_attrs())
                result.status = self.get_grpc_status[result.status]
                if text_data:
                    result.text = text_data.get("text", "")
                submission_list.submissions.append(result)

            self.logger.info(f"Retrieved {len(submissions)} submissions for lab_id={data['lab_id']}, page {data['page_number']} of size {data['page_size']}")

            return submission_list

    def UpdateSubmission(self, request, context) -> submissions_stub.Submission:
        """
        Update an existing submission.
        
        Args:
            request: UpdateSubmissionRequest containing:
                - submission_id (int): ID of the submission to update
                - status (Status, optional): New submission status (NOT_GRADED, IN_PROGRESS, ACCEPTED, REJECTED)
                - text (str, optional): New submission text content
            context: gRPC context
            
        Returns:
            submissions_stub.Submission: Updated submission data
            
        Raises:
            grpc.StatusCode.NOT_FOUND: If submission doesn't exist
        """

        self.logger.info(f"UpdateSubmission requested")

        data: dict = {
            "submission_id": request.submission_id,
            "status": request.status if request.HasField("status") else None,
            "text": request.text if request.HasField("text") else None
        }

        with Session(self.postgresql_engine) as session:
            stmt = select(Submission).where(Submission.id == data["submission_id"])
            submission = session.execute(stmt).scalar_one_or_none()

            if submission is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Submission not found")
                return submissions_stub.Submission()

            if data["status"] is not None:
                submission.status = self.get_db_status[data["status"]]

            session.commit()

            result = submissions_stub.Submission(**submission.get_attrs())
            result.status = data["status"] if data["status"] is not None else self.get_grpc_status[result.status]

            # Update text in MongoDB
            if data["text"] is not None:
                self.submissions_texts.update_one(
                    {"submission_id": str(submission.id)},
                    {"$set": {"text": data["text"]}},
                    upsert=True
                )
                result.text = data["text"]
            else:
                # Fetch text from MongoDB if not provided
                text_data = self.submissions_texts.find_one({"submission_id": str(submission.id)})
                if text_data:
                    result.text = text_data.get("text", "")

            self.logger.info(f"Updated Submission with id={submission.id}, status={submission.status}")

            return result

    def DeleteSubmission(self, request, context) -> submissions_stub.DeleteSubmissionResponse:
        """
        Delete a submission by ID.
        
        Args:
            request: DeleteSubmissionRequest containing:
                - submission_id (int): ID of the submission to delete
            context: gRPC context
            
        Returns:
            submissions_stub.DeleteSubmissionResponse: Success status of the deletion
            
        Raises:
            grpc.StatusCode.NOT_FOUND: If submission doesn't exist
            grpc.StatusCode.INTERNAL: If asset deletion from storage fails
        """

        self.logger.info(f"DeleteSubmission requested")

        data: dict = {
            "submission_id": request.submission_id
        }

        with Session(self.postgresql_engine) as session:
            stmt = select(Submission).where(Submission.id == data["submission_id"])
            submission = session.execute(stmt).scalar_one_or_none()

            if submission is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Submission not found")
                return submissions_stub.DeleteSubmissionResponse(success=False)

            session.delete(submission)
            session.commit()

            # Delete submission text from MongoDB
            self.submissions_texts.delete_many({"submission_id": str(submission.id)})

            # Remove all assets from MinIO
            try:
                for asset in submission.assets:
                    self.minio_client.remove_object('submissions', f"{submission.id}/{asset.filename}")
            except Exception as e:
                context.set_code(grpc.StatusCode.INTERNAL)
                context.set_details(f"Failed to delete assets from MinIO: {str(e)}")
                return submissions_stub.DeleteSubmissionResponse(success=False)

            self.logger.info(f"Deleted Submission with id={submission.id}, lab_id={submission.lab_id}")

            return submissions_stub.DeleteSubmissionResponse(success=True)
    
    def GetUsersSubmissions(self, request, context) -> submissions_stub.SubmissionList:
        """
        Retrieve a paginated list of submissions for a specific user.
        
        Args:
            request: GetUsersSubmissionsRequest containing:
                - user_id (int): ID of the user to get submissions for
                - page_number (int): Page number (1-based)
                - page_size (int): Number of submissions per page
            context: gRPC context
            
        Returns:
            submissions_stub.SubmissionList: List of submissions with total count
            
        Raises:
            grpc.StatusCode.INVALID_ARGUMENT: If page_number or page_size is None or <= 0
        """

        self.logger.info(f"GetUsersSubmissions requested")

        data: dict = {
            "user_id": request.user_id,
            "page_number": request.page_number,
            "page_size": request.page_size
        }

        if data["page_number"] is None or data["page_number"] <= 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(f"Page number is required, got '{data['page_number']}'")
            return submissions_stub.SubmissionList()

        if data["page_size"] is None or data["page_size"] <= 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(f"Page size is required, got '{data['page_size']}'")
            return submissions_stub.SubmissionList()

        with Session(self.postgresql_engine) as session:
            stmt = select(Submission).where(Submission.owner_id == data["user_id"]).offset((data["page_number"] - 1) * data["page_size"]).limit(data["page_size"])
            submissions = session.execute(stmt).scalars().all()
            
            submission_list = submissions_stub.SubmissionList(total_count=len(submissions))
            for submission in submissions:
                # Fetch text from MongoDB
                text_data = self.submissions_texts.find_one({"submission_id": str(submission.id)})
                result = submissions_stub.Submission(**submission.get_attrs())
                result.status = self.get_grpc_status[result.status]
                if text_data:
                    result.text = text_data.get("text", "")
                submission_list.submissions.append(result)

            self.logger.info(f"Retrieved {len(submissions)} submissions for user_id={data['user_id']}, page {data['page_number']} of size {data['page_size']}")

            return submission_list

    # Assets Management
    def UploadAsset(self, request_iterator, context) -> submissions_stub.Asset:
        """
        Upload a file asset for a submission using streaming.
        
        Args:
            request_iterator: Stream of UploadAssetRequest messages:
                - First message: UploadAssetMetadata containing:
                    - submission_id (int): ID of the submission to attach asset to
                    - filename (str): Name of the file
                    - filesize (int): Size of the file in bytes
                - Subsequent messages: File chunks as bytes
            context: gRPC context
            
        Returns:
            submissions_stub.Asset: Created asset with generated ID and upload timestamp
            
        Raises:
            grpc.StatusCode.INVALID_ARGUMENT: If first message doesn't contain metadata, or if filename is empty/None, or if filesize is None or <= 0
            grpc.StatusCode.NOT_FOUND: If submission doesn't exist
            grpc.StatusCode.INTERNAL: If file upload fails
        """

        self.logger.info(f"UploadAsset requested")

        metadata_request = next(request_iterator)

        if metadata_request.HasField('metadata'):
            data: dict = {
                "submission_id": metadata_request.metadata.submission_id,
                "filename": metadata_request.metadata.filename,
                "filesize": metadata_request.metadata.filesize
            }

            if data["filename"] is None or data["filename"] == "":
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details(f"Filename is required, got '{data['filename']}'")
                return submissions_stub.Asset()

            if data["filesize"] is None or data["filesize"] <= 0:
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details(f"Filesize is required, got '{data['filesize']}'")
                return submissions_stub.Asset()

        else:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details("First request must contain metadata")
            return submissions_stub.Asset()

        with Session(self.postgresql_engine) as session:
            # Check if submission exists
            stmt = select(Submission).where(Submission.id == data["submission_id"])
            submission = session.execute(stmt).scalar_one_or_none()

            if submission is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Submission not found")
                return submissions_stub.Asset()

            # Create new asset
            new_asset = SubmissionAsset(**data)
            session.add(new_asset)
            session.commit()

            # Put file in minio bucket
            try:
                # Download the file to a local path
                with open(f'files/{new_asset.filename}', 'wb') as f:
                    for request in request_iterator:
                        if request.HasField('chunk'):
                            f.write(request.chunk)
                        else:
                            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                            context.set_details("Subsequent requests must contain chunk data")
                            return submissions_stub.Asset()

                # Put the file in MinIO
                self.minio_client.fput_object(
                    "submissions",
                    f"{new_asset.submission_id}/{new_asset.filename}",
                    f'files/{new_asset.filename}'
                )

                # Clean up local file after upload
                os.remove(f'files/{new_asset.filename}')

            except Exception as e:
                context.set_code(grpc.StatusCode.INTERNAL)
                context.set_details(f"Failed to upload asset to MinIO: {str(e)}")
                return submissions_stub.Asset()

            self.logger.info(f"Uploaded asset with id={new_asset.id}, filename={new_asset.filename} for submission_id={new_asset.submission_id}")

            return submissions_stub.Asset(**new_asset.get_attrs())

    def UpdateAsset(self, request_iterator, context) -> submissions_stub.Asset:
        """
        Update an existing submission asset file using streaming.
        
        Args:
            request_iterator: Stream of UpdateAssetRequest messages:
                - First message: UpdateAssetMetadata containing:
                    - asset_id (int): ID of the asset to update
                    - filename (str): New filename
                    - filesize (int): New file size in bytes
                - Subsequent messages: New file chunks as bytes
            context: gRPC context
            
        Returns:
            submissions_stub.Asset: Updated asset data
            
        Raises:
            grpc.StatusCode.INVALID_ARGUMENT: If first message doesn't contain metadata, or if filename is empty/None, or if filesize is None or <= 0
            grpc.StatusCode.NOT_FOUND: If asset doesn't exist
            grpc.StatusCode.INTERNAL: If file upload fails
        """

        self.logger.info(f"UpdateAsset requested")

        metadata_request = next(request_iterator)

        if metadata_request.HasField('metadata'):
            data: dict = {
                "asset_id": metadata_request.metadata.asset_id,
                "filename": metadata_request.metadata.filename,
                "filesize": metadata_request.metadata.filesize
            }

            if data["filename"] is None or data["filename"] == "":
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details(f"Filename is required, got '{data['filename']}'")
                return submissions_stub.Asset()

            if data["filesize"] is None or data["filesize"] <= 0:
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details(f"Filesize is required, got '{data['filesize']}'")
                return submissions_stub.Asset()

        else:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details("First request must contain metadata")
            return submissions_stub.Asset()

        with Session(self.postgresql_engine) as session:
            # Check if asset exists
            stmt = select(SubmissionAsset).where(SubmissionAsset.id == data["asset_id"])
            asset = session.execute(stmt).scalar_one_or_none()

            if asset is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Asset not found")
                return submissions_stub.Asset()

            # Try to remove the old asset file from MinIO
            try:
                self.minio_client.remove_object('submissions', f"{asset.submission_id}/{asset.filename}")
            except Exception as e:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details(f"Failed to delete asset from MinIO: {str(e)}")
                return submissions_stub.Asset()

            asset.filename = data["filename"]
            asset.filesize = data["filesize"]

            session.commit()

            with open(f'files/{asset.filename}', 'wb') as f:
                for request in request_iterator:
                    if request.HasField('chunk'):
                        f.write(request.chunk)
                    else:
                        context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                        context.set_details("Subsequent requests must contain chunk data")
                        return submissions_stub.Asset()

            # Put the file in MinIO
            self.minio_client.fput_object(
                "submissions",
                f"{asset.submission_id}/{asset.filename}",
                f'files/{asset.filename}'
            )

            # Clean up local file after upload
            os.remove(f'files/{asset.filename}')

            self.logger.info(f"Updated asset with id={asset.id}, filename={asset.filename} for submission_id={asset.submission_id}")

            return submissions_stub.Asset(**asset.get_attrs())

    def DownloadAsset(self, request, context) -> submissions_stub.DownloadAssetResponse:
        """
        Download a submission asset file using streaming.
        
        Args:
            request: DownloadAssetRequest containing:
                - asset_id (int): ID of the asset to download
            context: gRPC context
            
        Returns:
            Generator yielding DownloadAssetResponse messages:
                - First message: Asset metadata
                - Subsequent messages: File chunks as bytes
                
        Raises:
            grpc.StatusCode.NOT_FOUND: If asset doesn't exist
            grpc.StatusCode.INTERNAL: If file download fails
        """

        self.logger.info(f"DownloadAsset requested")

        def response_messages():
            data: dict = {
                "asset_id": request.asset_id
            }

            with Session(self.postgresql_engine) as session:
                stmt = select(SubmissionAsset).where(SubmissionAsset.id == data["asset_id"])
                asset = session.execute(stmt).scalar_one_or_none()

                if asset is None:
                    context.set_code(grpc.StatusCode.NOT_FOUND)
                    context.set_details("Asset not found")
                    return submissions_stub.DownloadAssetResponse()

                yield submissions_stub.DownloadAssetResponse(asset=submissions_stub.Asset(**asset.get_attrs()))

                # Download the file from MinIO
                try:
                    self.minio_client.fget_object(
                        "submissions",
                        f"{asset.submission_id}/{asset.filename}",
                        f'files/{asset.filename}'
                    )
                except Exception as e:
                    context.set_code(grpc.StatusCode.INTERNAL)
                    context.set_details(f"Failed to download asset from MinIO: {str(e)}")
                    return submissions_stub.DownloadAssetResponse()

                with open(f'files/{asset.filename}', 'rb') as f:
                    while True:
                        chunk = f.read(8 * 1024)

                        if not chunk:
                            break

                        yield submissions_stub.DownloadAssetResponse(chunk=chunk)

                # Clean up local file after download
                os.remove(f'files/{asset.filename}')

        self.logger.info(f"Downloading asset with id={request.asset_id}")

        return response_messages()

    def DeleteAsset(self, request, context) -> submissions_stub.DeleteAssetResponse:
        """
        Delete a submission asset by ID.
        
        Args:
            request: DeleteAssetRequest containing:
                - asset_id (int): ID of the asset to delete
            context: gRPC context
            
        Returns:
            submissions_stub.DeleteAssetResponse: Success status of the deletion
            
        Raises:
            grpc.StatusCode.NOT_FOUND: If asset doesn't exist
            grpc.StatusCode.INTERNAL: If file deletion from storage fails
        """

        self.logger.info(f"DeleteAsset requested")

        data: dict = {
            "asset_id": request.asset_id
        }

        with Session(self.postgresql_engine) as session:
            stmt = select(SubmissionAsset).where(SubmissionAsset.id == data["asset_id"])
            asset = session.execute(stmt).scalar_one_or_none()

            if asset is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Asset not found")
                return submissions_stub.DeleteAssetResponse(success=False)

            session.delete(asset)
            session.commit()

            # Remove the asset from MinIO
            try:
                self.minio_client.remove_object('submissions', f"{asset.submission_id}/{asset.filename}")
            except Exception as e:
                context.set_code(grpc.StatusCode.INTERNAL)
                context.set_details(f"Failed to delete asset from MinIO: {str(e)}")
                return submissions_stub.DeleteAssetResponse(success=False)

            self.logger.info(f"Deleted asset with id={asset.id}, filename={asset.filename} for submission_id={asset.submission_id}")

            return submissions_stub.DeleteAssetResponse(success=True)

    def ListAssets(self, request, context) -> submissions_stub.AssetList:
        """
        List all assets for a specific submission.
        
        Args:
            request: ListAssetsRequest containing:
                - submission_id (int): ID of the submission to list assets for
            context: gRPC context
            
        Returns:
            submissions_stub.AssetList: List of assets with total count
            
        Raises:
            grpc.StatusCode.NOT_FOUND: If submission doesn't exist or has no assets
        """

        self.logger.info(f"ListAssets requested")

        data: dict = {
            "submission_id": request.submission_id
        }

        with Session(self.postgresql_engine) as session:
            stmt = select(Submission).where(Submission.id == data["submission_id"])
            submission = session.execute(stmt).scalar_one_or_none()

            if submission is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Submission not found")
                return submissions_stub.AssetList()

            if not submission.assets:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Submission has no assets")
                return submissions_stub.AssetList()

            asset_list = submissions_stub.AssetList()
            asset_list.total_count = len(submission.assets)
            asset_list.assets.extend([submissions_stub.Asset(**asset.get_attrs()) for asset in submission.assets])

            self.logger.info(f"Listed {len(submission.assets)} assets for submission_id={data['submission_id']}")

            return asset_list

if __name__ == "__main__":
    logger = logging.getLogger("__main__")
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    labs_service.add_LabServiceServicer_to_server(LabService(), server)
    submissions_service.add_SubmissionServiceServicer_to_server(SubmissionService(), server)

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
