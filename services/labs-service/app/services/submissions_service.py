# Import downloaded modules
import grpc
from sqlalchemy.orm import Session
from sqlalchemy import select

# Import built-in modules
import os
import logging

# Import project files
from config import Config
from utils.models import Lab, Submission, SubmissionAsset
import proto.submissions_service_pb2 as submissions_stub  # Generated from submissions_service.proto
import proto.submissions_service_pb2_grpc as submissions_service  # Generated from submissions_service.proto
from services.tools import Tools

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

class SubmissionService(submissions_service.SubmissionServiceServicer):
    def __init__(self):
        self.tools = Tools()

        self.minio_client = self.tools.get_minio_client()
        self.postgresql_engine = self.tools.get_postgresql_engine()

        mongo_client = self.tools.get_mongo_client()
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
                error_message = f"Lab with id '{data['lab_id']}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

                return submissions_stub.Submission()

            if lab.owner_id == data["owner_id"]:
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                error_message = f"User cannot submit to his own lab"
                context.set_details(error_message)
                
                self.logger.error(error_message)
                
                return submissions_stub.Submission()

            # Create new submission
            new_submission = Submission(**data)
            session.add(new_submission)

            lab.submissions += 1

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
                error_message = f"Submission with id '{data['submission_id']}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

                return submissions_stub.Submission()

            # Fetch text from MongoDB
            text_data = self.submissions_texts.find_one({"submission_id": str(submission.id)})

            result = submissions_stub.Submission(**submission.get_attrs())
            result.status = self.get_grpc_status[result.status]
            if text_data:
                result.text = text_data.get("text", "")

            self.logger.info(f"Retrieved Submission with id={submission.id}, lab_id={submission.lab_id}")

            self.logger.info(f"Submission: {result}")

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
            error_message = f"Page number is required, got '{data['page_number']}'"
            context.set_details(error_message)

            self.logger.error(error_message)

            return submissions_stub.SubmissionList()

        if data["page_size"] is None or data["page_size"] <= 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Page size is required, got '{data['page_size']}'"
            context.set_details(error_message)

            self.logger.error(error_message)

            return submissions_stub.SubmissionList()

        with Session(self.postgresql_engine) as session:
            # Check if lab exists
            stmt = select(Lab).where(Lab.id == data["lab_id"])
            lab = session.execute(stmt).scalar_one_or_none()

            if lab is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                error_message = f"Lab with id '{data['lab_id']}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

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
                error_message = f"Submission with id '{data['submission_id']}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

                return submissions_stub.Submission()

            if data["status"] is not None:
                if submission.status == self.get_db_status[submissions_stub.Status.NOT_GRADED] or submission.status == self.get_db_status[submissions_stub.Status.IN_PROGRESS]:
                    submission.status = self.get_db_status[data["status"]]
                else:
                    context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                    error_message = f"Submission with id '{data['submission_id']}' is already graded"
                    context.set_details(error_message)

                    self.logger.error(error_message)

                    return submissions_stub.Submission()

            session.commit()

            result = submissions_stub.Submission(**submission.get_attrs())
            result.status = self.get_grpc_status[result.status]

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
                error_message = f"Submission with id '{data['submission_id']}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

                return submissions_stub.DeleteSubmissionResponse(success=False)

            submission.lab.submissions -= 1

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
                error_message = f"Failed to delete assets from MinIO: {str(e)}"
                context.set_details(error_message)

                self.logger.error(error_message)

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
            error_message = f"Page number is required, got '{data['page_number']}'"
            context.set_details(error_message)

            self.logger.error(error_message)

            return submissions_stub.SubmissionList()

        if data["page_size"] is None or data["page_size"] <= 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Page size is required, got '{data['page_size']}'"
            context.set_details(error_message)

            self.logger.error(error_message)

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

    
    def GetPossibleToReviewSubmissions(self, request, context) -> submissions_stub.SubmissionList:
        data: dict = {
            "user_id": request.user_id
        }

        self.logger.info(f"GetPossibleToReviewSubmissions requested")

        with Session(self.postgresql_engine) as session:
            # Get all user submissions that have been accepted
            stmt = select(Submission).where(Submission.owner_id == data["user_id"]).where(Submission.status == self.get_db_status[submissions_stub.Status.ACCEPTED])
            submissions = session.execute(stmt).scalars().all()

            # Get all labs that the user owns
            stmt = select(Lab).where(Lab.owner_id == data["user_id"])
            labs = session.execute(stmt).scalars().all()

            # Combine unique lab ids from submissions and labs
            lab_ids = set([submission.lab_id for submission in submissions] + [lab.id for lab in labs])

            # Get all submissions that are not graded and not owned by the user
            stmt = select(Submission).where(Submission.lab_id.in_(lab_ids)).where(Submission.status == self.get_db_status[submissions_stub.Status.NOT_GRADED]).where(Submission.owner_id != data["user_id"])
            submissions = session.execute(stmt).scalars().all()

            # Create submission list
            submission_list = submissions_stub.SubmissionList(total_count=len(submissions))

            for submission in submissions:
                result = submissions_stub.Submission(**submission.get_attrs())
                result.status = self.get_grpc_status[result.status]
                # Fetch text from MongoDB
                text_data = self.submissions_texts.find_one({"submission_id": str(submission.id)})
                if text_data:
                    result.text = text_data.get("text", "")

                submission_list.submissions.append(result)

            self.logger.info(f"Retrieved {len(submissions)} submissions to review for user_id={data['user_id']}")

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
                error_message = f"Filename is required, got '{data['filename']}'"
                context.set_details(error_message)

                self.logger.error(error_message)

                return submissions_stub.Asset()

            if data["filesize"] is None or data["filesize"] <= 0:
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                error_message = f"Filesize is required, got '{data['filesize']}'"
                context.set_details(error_message)

                self.logger.error(error_message)

                return submissions_stub.Asset()

        else:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = "First request must contain metadata"
            context.set_details(error_message)

            self.logger.error(error_message)

            return submissions_stub.Asset()

        with Session(self.postgresql_engine) as session:
            # Check if submission exists
            stmt = select(Submission).where(Submission.id == data["submission_id"])
            submission = session.execute(stmt).scalar_one_or_none()

            if submission is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                error_message = f"Submission with id '{data['submission_id']}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

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
                            error_message = "Subsequent requests must contain chunk data"
                            context.set_details(error_message)

                            self.logger.error(error_message)

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
                error_message = f"Failed to upload asset to MinIO: {str(e)}"
                context.set_details(error_message)

                self.logger.error(error_message)

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
                error_message = f"Filename is required, got '{data['filename']}'"
                context.set_details(error_message)

                self.logger.error(error_message)

                return submissions_stub.Asset()

            if data["filesize"] is None or data["filesize"] <= 0:
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                error_message = f"Filesize is required, got '{data['filesize']}'"
                context.set_details(error_message)

                self.logger.error(error_message)

                return submissions_stub.Asset()

        else:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = "First request must contain metadata"
            context.set_details(error_message)

            self.logger.error(error_message)

            return submissions_stub.Asset()

        with Session(self.postgresql_engine) as session:
            # Check if asset exists
            stmt = select(SubmissionAsset).where(SubmissionAsset.id == data["asset_id"])
            asset = session.execute(stmt).scalar_one_or_none()

            if asset is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                error_message = f"Asset with id '{data['asset_id']}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

                return submissions_stub.Asset()

            # Try to remove the old asset file from MinIO
            try:
                self.minio_client.remove_object('submissions', f"{asset.submission_id}/{asset.filename}")
            except Exception as e:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                error_message = f"Failed to delete asset from MinIO: {str(e)}"
                context.set_details(error_message)

                self.logger.error(error_message)

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
                        error_message = "Subsequent requests must contain chunk data"
                        context.set_details(error_message)

                        self.logger.error(error_message)

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
                    error_message = f"Asset with id '{data['asset_id']}' not found"
                    context.set_details(error_message)

                    self.logger.error(error_message)

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
                    error_message = f"Failed to download asset from MinIO: {str(e)}"
                    context.set_details(error_message)

                    self.logger.error(error_message)

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
                error_message = f"Asset with id '{data['asset_id']}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

                return submissions_stub.DeleteAssetResponse(success=False)

            session.delete(asset)
            session.commit()

            # Remove the asset from MinIO
            try:
                self.minio_client.remove_object('submissions', f"{asset.submission_id}/{asset.filename}")
            except Exception as e:
                context.set_code(grpc.StatusCode.INTERNAL)
                error_message = f"Failed to delete asset from MinIO: {str(e)}"
                context.set_details(error_message)

                self.logger.error(error_message)

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
                error_message = f"Submission with id '{data['submission_id']}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

                return submissions_stub.AssetList()

            asset_list = submissions_stub.AssetList(total_count=len(submission.assets))
            asset_list.assets.extend([submissions_stub.Asset(**asset.get_attrs()) for asset in submission.assets])

            self.logger.info(f"Listed {len(submission.assets)} assets for submission_id={data['submission_id']}")

            return asset_list