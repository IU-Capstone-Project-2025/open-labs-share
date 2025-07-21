# Import downloaded modules
import grpc
from sqlalchemy.orm import Session
from sqlalchemy import select

# Import built-in modules
import os
import logging

# Import project files
from utils.models import Lab, LabAsset, ArticleRelation, Tag, LabTag
import proto.tags_service_pb2 as tags_stub # Generated from tags.proto
import proto.tags_service_pb2_grpc as tags_service # Generated from tags.proto
from services.tools import Tools


logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

class TagService(tags_service.TagServiceServicer):
    def __init__(self):
        self.logger = logging.getLogger(self.__class__.__name__)
        self.tools = Tools()

        self.engine = self.tools.get_postgresql_engine()

    # Tags Management
    def CreateTag(self, request, context) -> tags_stub.Tag:
        """
        Create a new tag.
        
        Args:
            request: CreateTagRequest containing:
                - name (str): Name of the tag
                - description (str): Description of the tag
            context: gRPC context
        
        Returns:
            tags_stub.Tag: The created tag with generated ID and timestamps, or empty Tag on error
        
        Errors:
            INVALID_ARGUMENT: If name is missing or empty
            ALREADY_EXISTS: If a tag with the same name already exists
        """

        self.logger.info(f"CreateTag requested")

        data: dict = {
            "name": request.name,
            "description": request.description,
        }

        if data["name"] is None or data["name"] == "":
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Name is required, got {data['name']}"
            context.set_details(error_message)

            self.logger.error(error_message)
            
            return tags_stub.Tag()

        with Session(self.engine) as session:
            stmt = select(Tag).where(Tag.name == data["name"])
            tag = session.execute(stmt).scalar_one_or_none()
            
            if tag is not None:
                context.set_code(grpc.StatusCode.ALREADY_EXISTS)
                error_message = f"Tag with name '{data['name']}' already exists"
                context.set_details(error_message)

                self.logger.error(error_message)

                return tags_stub.Tag()

            new_tag = Tag(**data)
            session.add(new_tag)
            session.commit()

            self.logger.info(f"Tag created: {new_tag.get_attrs()}")
            
            return tags_stub.Tag(**new_tag.get_attrs())
            

    def GetTag(self, request, context) -> tags_stub.Tag:
        """
        Get a tag by tag_id.
        
        Args:
            request: GetTagRequest containing:
                - id (int): ID of the tag to retrieve
            context: gRPC context
        
        Returns:
            tags_stub.Tag: Tag data if found, otherwise empty Tag
        
        Errors:
            INVALID_ARGUMENT: If tag ID is missing
            NOT_FOUND: If the tag does not exist
        """

        data: dict = {
            "tag_id": request.id
        }

        self.logger.info(f"GetTag requested")

        if data["tag_id"] is None:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Tag id is required, got {data['tag_id']}"
            context.set_details(error_message)

            self.logger.error(error_message)
            
            return tags_stub.Tag()

        with Session(self.engine) as session:
            stmt = select(Tag).where(Tag.id == data["tag_id"])
            tag = session.execute(stmt).scalar_one_or_none()

            if tag is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                error_message = f"Tag with id '{data['tag_id']}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

                return tags_stub.Tag()

            self.logger.info(f"Tag found: {tag.get_attrs()}")

            return tags_stub.Tag(**tag.get_attrs())


    def GetTags(self, request, context) -> tags_stub.TagList:
        """
        Get a list of tags (paginated).
        
        Args:
            request: GetTagsRequest containing:
                - page_number (int): Page number (1-based)
                - page_size (int): Number of tags per page
            context: gRPC context
        
        Returns:
            tags_stub.TagList: List of tags and count, or empty TagList on error
        
        Errors:
            INVALID_ARGUMENT: If page_number or page_size is invalid (≤ 0)
        """

        data: dict = {
            "page_number": request.page_number,
            "page_size": request.page_size,
        }

        self.logger.info(f"GetTags requested")

        if data["page_number"] is None or data["page_number"] <= 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Page number must be greater than 0, got {data['page_number']}"
            context.set_details(error_message)

            self.logger.error(error_message)

            return tags_stub.TagList()

        if data["page_size"] is None or data["page_size"] <= 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Page size must be greater than 0, got {data['page_size']}"
            context.set_details(error_message)

            self.logger.error(error_message)

            return tags_stub.TagList()

        with Session(self.engine) as session:
            stmt = select(Tag).order_by(Tag.id.desc()).offset((data["page_number"] - 1) * data["page_size"]).limit(data["page_size"])
            tags = session.execute(stmt).scalars().all()

            tags_list = tags_stub.TagList(count=len(tags))
            for tag in tags:
                tags_list.tags.append(tags_stub.Tag(**tag.get_attrs()))

            self.logger.info(f"Tags retrieved: {len(tags)}")

            return tags_list

    
    def GetTagsByIds(self, request, context) -> tags_stub.TagList:
        """
        Get a list of tags by tag_ids.
        
        Args:
            request: GetTagsByIdsRequest containing:
                - ids (list[int]): List of tag IDs to retrieve
            context: gRPC context
        
        Returns:
            tags_stub.TagList: List of tags and count, or empty TagList on error
        
        Errors:
            INVALID_ARGUMENT: If tag IDs list is empty or missing
            NOT_FOUND: If any tag ID does not exist
        """

        data: dict = {
            "tag_ids": request.ids
        }

        self.logger.info(f"GetTagsByIds requested")

        if data["tag_ids"] is None or len(data["tag_ids"]) == 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Tag ids are required, got {data['tag_ids']}"
            context.set_details(error_message)

            self.logger.error(error_message)

            return tags_stub.TagList()
        
        with Session(self.engine) as session:
            tags_list = tags_stub.TagList(count=0)

            for tag_id in data["tag_ids"]:
                stmt = select(Tag).where(Tag.id == tag_id)
                tag = session.execute(stmt).scalar_one_or_none()

                if tag is None:
                    context.set_code(grpc.StatusCode.NOT_FOUND)
                    error_message = f"Tag with id '{tag_id}' not found"
                    context.set_details(error_message)

                    self.logger.error(error_message)

                    return tags_stub.TagList()

                tags_list.tags.append(tags_stub.Tag(**tag.get_attrs()))
                tags_list.count += 1

            self.logger.info(f"Tags retrieved: {tags_list.count}")

            return tags_list


    def UpdateTag(self, request, context) -> tags_stub.Tag:
        """
        Update a tag by tag_id.
        
        Args:
            request: UpdateTagRequest containing:
                - id (int): ID of the tag to update
                - name (str, optional): New tag name
                - description (str, optional): New tag description
            context: gRPC context
        
        Returns:
            tags_stub.Tag: Updated tag data, or empty Tag on error
        
        Errors:
            INVALID_ARGUMENT: If name is provided but empty
            NOT_FOUND: If the tag does not exist
        """

        self.logger.info(f"UpdateTag requested")

        data: dict = {
            "tag_id": request.id,
            "name": request.name if request.HasField("name") else None,
            "description": request.description if request.HasField("description") else None,
        }
        
        if data["name"] is not None and data["name"] == "":
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Name is required, got {data['name']}"
            context.set_details(error_message)

            self.logger.error(error_message)
            
            return tags_stub.Tag()

        with Session(self.engine) as session:
            stmt = select(Tag).where(Tag.id == data["tag_id"])
            tag = session.execute(stmt).scalar_one_or_none()

            if tag is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                error_message = f"Tag with id '{data['tag_id']}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

                return tags_stub.Tag()
            
            if data["name"] is not None:
                tag.name = data["name"]

            if data["description"] is not None:
                tag.description = data["description"]

            session.commit()

            self.logger.info(f"Updated Tag with id={tag.id}, name={tag.name}")

            return tags_stub.Tag(**tag.get_attrs())

    def DeleteTag(self, request, context) -> tags_stub.DeleteTagResponse:
        """
        Delete a tag by tag_id.
        
        Args:
            request: DeleteTagRequest containing:
                - id (int): ID of the tag to delete
            context: gRPC context
        
        Returns:
            tags_stub.DeleteTagResponse: Success status of the deletion
        
        Errors:
            NOT_FOUND: If the tag does not exist
        """
        
        self.logger.info(f"DeleteTag requested")

        data: dict = {
            "tag_id": request.id
        }

        with Session(self.engine) as session:
            stmt = select(Tag).where(Tag.id == data["tag_id"])
            tag = session.execute(stmt).scalar_one_or_none()

            if tag is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                error_message = f"Tag with id '{data['tag_id']}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

                return tags_stub.DeleteTagResponse(success=False)
            
            session.delete(tag)
            session.commit()

            self.logger.info(f"Tag deleted: {tag.id}")
            
            return tags_stub.DeleteTagResponse(success=True)

