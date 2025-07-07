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
        """

        data: dict = {
            "tag_id": request.tag_id
        }

        self.logger.info(f"GetTag requested")

        if data["tag_id"] is None:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Tag id is required, got {data['tag_id']}"
            context.set_details(error_message)

            self.logger.error(error_message)
            
            return tags_stub.Tag()

        with Session(self.engine) as session:
            stmt = select(Tag).where(Tag.id == request.tag_id)
            tag = session.execute(stmt).scalar_one_or_none()

            if tag is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                error_message = f"Tag with id '{request.tag_id}' not found"
                context.set_details(error_message)

                self.logger.error(error_message)

                return tags_stub.Tag()

            self.logger.info(f"Tag found: {tag.get_attrs()}")

            return tags_stub.Tag(**tag.get_attrs())


    def GetTags(self, request, context) -> tags_stub.TagList:
        """
        Get a list of tags (paginated).
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

            tags_list = tags_stub.TagList(total_count=len(tags))
            for tag in tags:
                tags_list.tags.append(tags_stub.Tag(**tag.get_attrs()))

            self.logger.info(f"Tags retrieved: {len(tags)}")

            return tags_list

    
    def GetTagsByIds(self, request, context) -> tags_stub.TagList:
        """
        Get a list of tags by tag_ids.
        """

        data: dict = {
            "tag_ids": request.tag_ids
        }

        self.logger.info(f"GetTagsByIds requested")

        if data["tag_ids"] is None or len(data["tag_ids"]) == 0:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            error_message = f"Tag ids are required, got {data['tag_ids']}"
            context.set_details(error_message)

            self.logger.error(error_message)

            return tags_stub.TagList()
        
        with Session(self.engine) as session:
            stmt = select(Tag).where(Tag.id.in_(data["tag_ids"]))
            tags = session.execute(stmt).scalars().all()

            tags_list = tags_stub.TagList(total_count=len(tags))
            for tag in tags:
                tags_list.tags.append(tags_stub.Tag(**tag.get_attrs()))

            self.logger.info(f"Tags retrieved: {len(tags)}")

            return tags_list


    def UpdateTag(self, request, context) -> tags_stub.Tag:
        """
        Update a tag by tag_id.
        """

        self.logger.info(f"UpdateTag requested")

        data: dict = {
            "tag_id": request.tag_id,
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
        """
        
        self.logger.info(f"DeleteTag requested")

        data: dict = {
            "tag_id": request.tag_id
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

