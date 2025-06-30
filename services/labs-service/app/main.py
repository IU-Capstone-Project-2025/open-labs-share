# Import downloaded modules
import grpc
import minio
from pymongo import MongoClient
from sqlalchemy.orm import Session
from sqlalchemy import create_engine, select
from google.protobuf.timestamp_pb2 import Timestamp

# Import built-in modules
from concurrent import futures
import sys
import os

# Fixes import path for proto files
sys.path.append(os.path.join(os.path.dirname(__file__), "proto"))

# Import project files
from config import Config
from utils.models import Lab, LabAsset, ArticleRelation, Submission, SubmissionAsset
import proto.labs_service_pb2 as labs_stub # Generated from labs.proto
import proto.labs_service_pb2_grpc as labs_service # Generated from labs.proto
import proto.submissions_service_pb2 as submissions_stub  # Generated from submissions_service.proto
import proto.submissions_service_pb2_grpc as submissions_service  # Generated from submissions_service.proto


class Tools:
    _minio_client: minio.Minio = None
    _mongo_client: MongoClient = None
    _postgresql_engine = None

    def get_minio_client(self) -> minio.Minio:
        if self._minio_client is None:
            print(f"Connection to MinIO at {Config.MINIO_ENDPOINT}")
            self._minio_client = minio.Minio(
                endpoint=Config.MINIO_ENDPOINT,
                access_key=Config.MINIO_ACCESS_KEY,
                secret_key=Config.MINIO_SECRET_KEY,
                secure=False
            )
        return self._minio_client

    def get_mongo_client(self) -> MongoClient:
        if self._mongo_client is None:
            url = f"mongodb://{Config.MONGODB_HOST}:{Config.MONGODB_PORT}/"

            print(f"Connecting to MongoDB at {url}")
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

            print(f"Connecting to PostgreSQL at {url}")
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

        if not self.minio_client.bucket_exists("labs"):
            self.minio_client.make_bucket("labs")

        # Ensure the temporary files directory exists
        if not os.path.exists('files'):
            os.makedirs('files')

    # ------- Labs Management -------
    def CreateLab(self, request, context) -> labs_stub.Lab:
        data: dict = {
            "owner_id": request.owner_id,
            "title": str(request.title),
            "abstract": str(request.abstract),
        }

        with Session(self.engine) as session:
            new_lab = Lab(**data)
            session.add(new_lab)

            if request.HasField("related_articles"):
                article_ids = request.related_articles.article_id
                new_lab.articles.extend(article_ids)

            session.commit()

            print(new_lab.get_attrs())

            return labs_stub.Lab(**new_lab.get_attrs())


    def GetLab(self, request, context) -> labs_stub.Lab:
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

            return labs_stub.Lab(**lab.get_attrs())


    def GetLabs(self, request, context) -> labs_stub.LabList:
        page_number = request.page_number
        page_size = request.page_size

        with Session(self.engine) as session:
            # Get total count of labs
            total_count = session.query(Lab).count()

            # Get paginated labs
            stmt = select(Lab).offset((page_number - 1) * page_size).limit(page_size)
            labs = session.execute(stmt).scalars().all()

            lab_list = labs_stub.LabList(total_count=total_count)
            for lab in labs:
                lab_list.labs.append(labs_stub.Lab(**lab.get_attrs()))

            return lab_list


    def UpdateLab(self, request, context) -> labs_stub.Lab:
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
            return labs_stub.Lab(**lab.get_attrs())


    def DeleteLab(self, request, context) -> labs_stub.DeleteLabResponse:
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
            return labs_stub.DeleteLabResponse(success=True)

    # ------- Lab Assets Management -------
    def UploadAsset(self, request_iterator, context) -> labs_stub.Asset:
        # Check for metadata being the first request
        metadata_request = next(request_iterator)

        if metadata_request.HasField('metadata'):
            data: dict = {
                "lab_id": metadata_request.metadata.lab_id,
                "filename": metadata_request.metadata.filename,
                "filesize": metadata_request.metadata.filesize
            }
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
            return labs_stub.Asset(**new_asset.get_attrs())


    def UpdateAsset(self, request_iterator, context) -> labs_stub.Asset:
        # Check for metadata being the first request
        metadata_request = next(request_iterator)

        if metadata_request.HasField('metadata'):
            data: dict = {
                "asset_id": metadata_request.metadata.asset_id,
                "filename": metadata_request.metadata.filename,
                "filesize": metadata_request.metadata.filesize
            }
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
            return labs_stub.Asset(**lab_asset.get_attrs())


    def DownloadAsset(self, request, context) -> labs_stub.DownloadAssetResponse:
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

        return response_messages()


    def DeleteAsset(self, request, context) -> labs_stub.DeleteAssetResponse:
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

            return labs_stub.DeleteAssetResponse(success=True)


    def ListAssets(self, request, context) -> labs_stub.AssetList:
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

            return asset_list


class SubmissionService(submissions_service.SubmissionServiceServicer):
    def __init__(self):
        self.minio_client = tools.get_minio_client()
        self.postgresql_engine = tools.get_postgresql_engine()
        mongo_client = tools.get_mongo_client()
        self.submissions_texts = mongo_client[Config.MONGODB_NAME]["submissions_texts"]

        if not self.minio_client.bucket_exists("submissions"):
            self.minio_client.make_bucket("submissions")

        # Ensure the temporary files directory exists
        if not os.path.exists('files'):
            os.makedirs('files')

    # Submissions Management
    def CreateSubmission(self, request, context) -> submissions_stub.Submission:
        data: dict = {
            "lab_id": request.lab_id,
            "owner_id": request.owner_id,
        }

        text = request.text

        print(f"data: {data}")
        print(f"text: {text}")

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

            return result

    def GetSubmission(self, request, context) -> submissions_stub.Submission:
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
            if text_data:
                result.text = text_data.get("text", "")

            return result

    def GetSubmissions(self, request, context) -> submissions_stub.SubmissionList:
        data: dict = {
            "lab_id": request.lab_id,
            "page_number": request.page_number,
            "page_size": request.page_size
        }

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
                if text_data:
                    result.text = text_data.get("text", "")
                submission_list.submissions.append(result)

            return submission_list

    def UpdateSubmission(self, request, context) -> submissions_stub.Submission:
        data: dict = {
            "submission_id": request.submission_id,
            "status": request.status if request.HasField("status") else None,
            "points": request.points if request.HasField("points") else None,
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
                submission.status = data["status"]

            if data["points"] is not None:
                submission.points = data["points"]

            session.commit()

            result = submissions_stub.Submission(**submission.get_attrs())

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

            return result

    def DeleteSubmission(self, request, context) -> submissions_stub.DeleteSubmissionResponse:
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

            return submissions_stub.DeleteSubmissionResponse(success=True)

    # Assets Management
    def UploadAsset(self, request_iterator, context) -> submissions_stub.Asset:
        metadata_request = next(request_iterator)

        if metadata_request.HasField('metadata'):
            data: dict = {
                "submission_id": metadata_request.metadata.submission_id,
                "filename": metadata_request.metadata.filename,
                "filesize": metadata_request.metadata.filesize
            }
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

            return submissions_stub.Asset(**new_asset.get_attrs())

    def UpdateAsset(self, request_iterator, context) -> submissions_stub.Asset:
        metadata_request = next(request_iterator)

        if metadata_request.HasField('metadata'):
            data: dict = {
                "asset_id": metadata_request.metadata.asset_id,
                "filename": metadata_request.metadata.filename,
                "filesize": metadata_request.metadata.filesize
            }
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

            return submissions_stub.Asset(**asset.get_attrs())

    def DownloadAsset(self, request, context):
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

        return response_messages()

    def DeleteAsset(self, request, context) -> submissions_stub.DeleteAssetResponse:
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

            return submissions_stub.DeleteAssetResponse(success=True)

    def ListAssets(self, request, context) -> submissions_stub.AssetList:
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

            return asset_list

if __name__ == "__main__":
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    labs_service.add_LabServiceServicer_to_server(LabService(), server)
    submissions_service.add_SubmissionServiceServicer_to_server(SubmissionService(), server)

    server_address = f"{Config.SERVICE_HOST}:{Config.SERVICE_PORT}"
    server.add_insecure_port(server_address)
    print(f"Starting gRPC server on {server_address}")
    server.start()
    try:
        server.wait_for_termination()
    except KeyboardInterrupt:
        print("Server is shutting down...")
        server.stop(0)