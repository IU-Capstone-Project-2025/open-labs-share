# Import downloaded modules
import grpc
from sqlalchemy.orm import Session
from sqlalchemy import select

# Import built-in modules
import os
import logging

# Import project files
from utils.models import Lab, LabAsset, ArticleRelation, Tag, LabTag
import proto.labs_service_pb2 as labs_stub # Generated from labs.proto
import proto.labs_service_pb2_grpc as labs_service # Generated from labs.proto
from services.tools import Tools

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)


class LabService(labs_service.LabServiceServicer):
    def __init__(self):
        self.tools = Tools()

        self.engine = self.tools.get_postgresql_engine()
        self.minio_client = self.tools.get_minio_client()
        self.logger = logging.getLogger(self.__class__.__name__)

        if not self.minio_client.bucket_exists("labs"):
            self.minio_client.make_bucket("labs")

        # Ensure the temporary files directory exists
        if not os.path.exists('files'):
            os.makedirs('files')


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
            error_message = f"Title is required, got '{data['title']}'"
            context.set_details(error_message)
            
            self.logger.error(error_message)
            
            return labs_stub.Lab()
        
        if data["abstract"] is None or data["abstract"] == "":
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Abstract is required, got '{data['abstract']}'"
            context.set_details(error_message)
            
            self.logger.error(error_message)
            
            return labs_stub.Lab()

        with Session(self.engine) as session:
            new_lab = Lab(**data)
            session.add(new_lab)

            if request.HasField("related_articles"):
                article_ids = request.related_articles.article_id
                new_lab.articles.extend([ArticleRelation(lab_id=new_lab.id, article_id=article_id) for article_id in article_ids])

            if request.HasField("tags"):
                tags_ids = request.tags.tag_ids

                for tag_id in tags_ids:
                    stmt = select(Tag).where(Tag.id == tag_id)
                    tag = session.execute(stmt).scalar_one_or_none()

                    if tag is None:
                        context.set_code(grpc.StatusCode.NOT_FOUND)
                        error_message = f"Tag with id '{tag_id}' not found"
                        context.set_details(error_message)
                        
                        self.logger.error(error_message)
                        
                        return labs_stub.Lab()

                    new_lab.tags.append(LabTag(lab_id=new_lab.id, tag_id=tag.id))
                    tag.labs_count += 1

            session.commit()

            self.logger.info(f"Created Lab with id={new_lab.id}, title={new_lab.title}")

            response_data = new_lab.get_attrs()
            response_data["related_articles"] = labs_stub.ArticleList(total_count=len(response_data["related_articles"]), article_id=response_data["related_articles"])
            response_data["tags"] = labs_stub.LabTagList(total_count=len(response_data["tags"]), tag_ids=response_data["tags"])

            response_lab = labs_stub.Lab(**response_data)

            return response_lab


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
                error_message = f"Lab with id '{data['lab_id']}' not found"
                context.set_details(error_message)
                
                self.logger.error(error_message)
                
                return labs_stub.Lab()

            self.logger.info(f"Retrieved Lab with id={lab.id}, title={lab.title}")

            response_lab_data = lab.get_attrs()
            response_lab_data["related_articles"] = labs_stub.ArticleList(total_count=len(response_lab_data["related_articles"]), article_id=response_lab_data["related_articles"])
            response_lab_data["tags"] = labs_stub.LabTagList(total_count=len(response_lab_data["tags"]), tag_ids=response_lab_data["tags"])

            response_lab = labs_stub.Lab(**response_lab_data)

            return response_lab


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
            error_message = f"Page number is required, got '{data['page_number']}'"
            context.set_details(error_message)

            self.logger.error(error_message)
            
            return labs_stub.LabList()
        
        if data["page_size"] is None or data["page_size"] <= 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Page size is required, got '{data['page_size']}'"
            context.set_details(error_message)

            self.logger.error(error_message)
            
            return labs_stub.LabList()

        with Session(self.engine) as session:
            # Get total count of labs
            total_count = session.query(Lab).count()

            # Get paginated labs
            stmt = select(Lab).offset((data["page_number"] - 1) * data["page_size"]).limit(data["page_size"])
            labs = session.execute(stmt).scalars().all()

            lab_list = labs_stub.LabList(total_count=total_count)
            for lab in labs:
                response_lab_data = lab.get_attrs()
                response_lab_data["related_articles"] = labs_stub.ArticleList(total_count=len(response_lab_data["related_articles"]), article_id=response_lab_data["related_articles"])
                response_lab_data["tags"] = labs_stub.LabTagList(total_count=len(response_lab_data["tags"]), tag_ids=response_lab_data["tags"])
                response_lab = labs_stub.Lab(**response_lab_data)
                lab_list.labs.append(response_lab)

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
            "related_articles": request.related_articles if request.HasField("related_articles") else None,
            "tags": request.tags if request.HasField("tags") else None
        }

        if data["title"] is not None and data["title"] == "":
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Title is required, got '{data['title']}'"
            context.set_details(error_message)

            self.logger.error(error_message)
            
            return labs_stub.Lab()

        with Session(self.engine) as session:
            stmt = select(Lab).where(Lab.id == data["lab_id"])
            lab = session.execute(stmt).scalar_one_or_none()

            if lab is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                error_message = f"Lab with id '{data['lab_id']}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

                return labs_stub.Lab()

            if data["title"] is not None:
                lab.title = data["title"]

            if data["abstract"] is not None:
                lab.abstract = data["abstract"]

            if data["related_articles"] is not None:
                article_ids = data["related_articles"].article_id
                lab.articles.clear()
                lab.articles.extend([ArticleRelation(lab_id=lab.id, article_id=article_id) for article_id in article_ids])

            if data["tags"] is not None:
                tags_ids = data["tags"].tag_ids

                exiisting_lab_tags = list(lab.tags)

                for lab_tag in exiisting_lab_tags:
                    stmt = select(Tag).where(Tag.id == lab_tag.tag_id)
                    tag = session.execute(stmt).scalar_one_or_none()

                    if tag is None:
                        context.set_code(grpc.StatusCode.NOT_FOUND)
                        error_message = f"Tag with id '{lab_tag.tag_id}' not found"
                        context.set_details(error_message)

                        self.logger.error(error_message)

                        return labs_stub.Lab()

                    tag.labs_count -= 1
                    session.delete(lab_tag)
                
                for tag_id in tags_ids:
                    stmt = select(Tag).where(Tag.id == tag_id)
                    tag = session.execute(stmt).scalar_one_or_none()

                    if tag is None:
                        context.set_code(grpc.StatusCode.NOT_FOUND)
                        error_message = f"Tag with id '{tag_id}' not found"
                        context.set_details(error_message)

                        self.logger.error(error_message)

                        return labs_stub.Lab()
                    
                    lab.tags.append(LabTag(lab_id=lab.id, tag_id=tag.id))
                    tag.labs_count += 1

            session.commit()

            self.logger.info(f"Updated Lab with id={lab.id}, title={lab.title}")

            response_lab_data = lab.get_attrs()
            response_lab_data["related_articles"] = labs_stub.ArticleList(total_count=len(response_lab_data["related_articles"]), article_id=response_lab_data["related_articles"])
            response_lab_data["tags"] = labs_stub.LabTagList(total_count=len(response_lab_data["tags"]), tag_ids=response_lab_data["tags"])
            response_lab = labs_stub.Lab(**response_lab_data)

            return response_lab


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
                error_message = f"Lab with id '{data['lab_id']}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

                return labs_stub.DeleteLabResponse(success=False)

            session.delete(lab)
            session.commit()

            # Remove all assets from MinIO
            try:
                for asset in lab.assets:
                    self.minio_client.remove_object('labs', f"{lab.id}/{asset.filename}")
            except Exception as e:
                context.set_code(grpc.StatusCode.INTERNAL)
                error_message = f"Failed to delete assets from MinIO: {str(e)}"
                context.set_details(error_message)

                self.logger.error(error_message)

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
            data = {
                "lab_id": metadata_request.metadata.lab_id,
                "filename": metadata_request.metadata.filename,
                "filesize": metadata_request.metadata.filesize
            }

            if data["filename"] is None or data["filename"] == "":
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                error_message = f"Filename is required, got '{data['filename']}'"
                context.set_details(error_message)

                self.logger.error(error_message)

                return labs_stub.Asset()

            if data["filesize"] is None or data["filesize"] <= 0:
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                error_message = f"Filesize is required, got '{data['filesize']}'"
                context.set_details(error_message)

                self.logger.error(error_message)

                return labs_stub.Asset()

        else:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = "First request must contain metadata"
            context.set_details(error_message)

            self.logger.error(error_message)

            return labs_stub.Asset()

        with Session(self.engine) as session:
            # Check if lab exists
            stmt = select(Lab).where(Lab.id == data["lab_id"])
            lab = session.execute(stmt).scalar_one_or_none()

            if lab is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                error_message = f"Lab with id '{data['lab_id']}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

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
                            error_message = "Subsequent requests must contain chunk data"
                            context.set_details(error_message)

                            self.logger.error(error_message)
                            
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
                error_message = f"Failed to upload asset to MinIO: {str(e)}"
                context.set_details(error_message)

                self.logger.error(error_message)

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
                error_message = f"Asset ID is required, got '{data['asset_id']}'"
                context.set_details(error_message)

                self.logger.error(error_message)

                return labs_stub.Asset()

            if data["filename"] is None or data["filename"] == "":
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                error_message = f"Filename is required, got '{data['filename']}'"
                context.set_details(error_message)

                self.logger.error(error_message)

                return labs_stub.Asset()

            if data["filesize"] is None or data["filesize"] <= 0:
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                error_message = f"Filesize is required, got '{data['filesize']}'"
                context.set_details(error_message)

                self.logger.error(error_message)

                return labs_stub.Asset()

        else:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = "First request must contain metadata"
            context.set_details(error_message)

            self.logger.error(error_message)

            return labs_stub.Asset()

        with Session(self.engine) as session:
            # Check if lab exists
            stmt = select(LabAsset).where(LabAsset.id == data["asset_id"])
            lab_asset = session.execute(stmt).scalar_one_or_none()

            if lab_asset is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                error_message = f"Asset with id '{data['asset_id']}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

                return labs_stub.Asset()

            # Try to remove the old asset file from MinIO
            try:
                self.minio_client.remove_object('labs', f"{lab_asset.lab_id}/{lab_asset.filename}")
            except Exception as e:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                error_message = f"Failed to delete asset from MinIO: {str(e)}"
                context.set_details(error_message)

                self.logger.error(error_message)

                return labs_stub.Asset()

            lab_asset.filename = data["filename"]
            lab_asset.filesize = data["filesize"]


            with open(f'files/{lab_asset.filename}', 'wb') as f:
                for request in request_iterator:
                    if request.HasField('chunk'):
                        f.write(request.chunk)
                    else:
                        context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                        error_message = "Subsequent requests must contain chunk data"
                        context.set_details(error_message)

                        self.logger.error(error_message)

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
                    error_message = f"Asset with id '{data['asset_id']}' not found"
                    context.set_details(error_message)

                    self.logger.error(error_message)

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
                    error_message = f"Failed to download asset from MinIO: {str(e)}"
                    context.set_details(error_message)

                    self.logger.error(error_message)

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
                error_message = f"Asset with id '{data['asset_id']}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

                return labs_stub.DeleteAssetResponse(success=False)

            session.delete(asset)

            # Remove the asset from MinIO
            try:
                self.minio_client.remove_object('labs', f"{asset.lab_id}/{asset.filename}")
            except Exception as e:
                context.set_code(grpc.StatusCode.INTERNAL)
                error_message = f"Failed to delete asset from MinIO: {str(e)}"
                context.set_details(error_message)

                self.logger.error(error_message)

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
                error_message = f"Lab with id '{data['lab_id']}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

                return labs_stub.AssetList()

            asset_list = labs_stub.AssetList()
            asset_list.total_count = len(lab.assets)
            asset_list.assets.extend([labs_stub.Asset(**asset.get_attrs()) for asset in lab.assets])

            self.logger.info(f"Listed {len(lab.assets)} assets for lab_id={data['lab_id']}")

            return asset_list
