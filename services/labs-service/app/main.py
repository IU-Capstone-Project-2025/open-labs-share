# Import downloaded modules
import grpc
from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session

# Import project files
from config import Config
import proto.labs_pb2 as stub # Generated from labs.proto
import proto.labs_pb2_grpc as service # Generated from labs.proto
from utils.models import Lab, LabAsset

class LabService(service.LabServiceServicer):
    def __init__(self):
        user = Config.DB_USER
        password = Config.DB_PASSWORD
        host = Config.DB_HOST
        port = Config.DB_PORT
        db_name = Config.DB_NAME
        url = f"postgresql://{user}:{password}@{host}:{port}/{db_name}"

        self.engine = create_engine(url, echo=True)

    # Labs Management
    def CreateLab(self, request, context) -> stub.Lab:
        data: dict = {
            "owner_id": request.owner_id,
            "title": request.title,
            "abstract": request.abstract
        }

        with Session(self.engine) as session:
            new_lab = Lab(**data)
            session.add(new_lab)

            if hasattr(request, "related_articles"):
                article_ids = request.related_articles.article_id
                new_lab.articles.extend(article_ids)

            session.commit()

            return stub.Lab(**new_lab.get_attrs())

    def GetLab(self, request, context) -> stub.Lab:
        data: dict = {
            "lab_id": request.lab_id
        }

        with Session(self.engine) as session:
            stmt = select(Lab).where(Lab.id == data["lab_id"])
            lab = session.execute(stmt).scalar_one_or_none()

            if lab is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Lab not found")
                return stub.Lab()

            return stub.Lab(**lab.get_attrs())

    def GetLabs(self, request, context) -> stub.LabList:
        data: dict = {
            "page_number": request.page_number,
            "page_size": request.page_size
        }

        with Session(self.engine) as session:
            stmt = select(Lab).offset((data["page_number"] - 1) * data["page_size"]).limit(data["page_size"])
            labs = session.execute(stmt).scalars().all()

            lab_list = stub.LabList(total_count=len(labs))
            for lab in labs:
                lab_list.labs.append(stub.Lab(**lab.get_attrs()))

            return lab_list

    def UpdateLab(self, request, context) -> stub.Lab:
        data: dict = {
            "lab_id": request.lab_id,
            "title": request.title if hasattr(request, "title") else None,
            "abstract": request.abstract if hasattr(request, "abstract") else None,
            "related_articles": request.related_articles if hasattr(request, "related_articles") else None
        }

        with Session(self.engine) as session:
            stmt = select(Lab).where(Lab.id == data["lab_id"])
            lab = session.execute(stmt).scalar_one_or_none()

            if lab is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Lab not found")
                return stub.Lab()

            if data["title"] is not None:
                lab.title = data["title"]

            if data["abstract"] is not None:
                lab.abstract = data["abstract"]

            if data["related_articles"] is not None:
                article_ids = data["related_articles"].article_id
                lab.articles.clear()
                lab.articles.extend(article_ids)

            session.commit()
            return stub.Lab(**lab.get_attrs())

    def DeleteLab(self, request, context) -> stub.DeleteLabResponse:
        data: dict = {
            "lab_id": request.lab_id
        }

        with Session(self.engine) as session:
            stmt = select(Lab).where(Lab.id == data["lab_id"])
            lab = session.execute(stmt).scalar_one_or_none()

            if lab is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Lab not found")
                return stub.DeleteLabResponse(success=False)

            session.delete(lab)
            session.commit()
            return stub.DeleteLabResponse(success=True)

    # Assets Management
    def UploadAsset(self, request_iterator, context) -> stub.Asset:
        # Example for extracting variables from the stream:
        # for req in request_iterator:
        #     data: dict = {
        #         "lab_id": req.lab_id if req.HasField("lab_id") else None,
        #         "filename": req.filename if req.HasField("filename") else None,
        #         "total_size": req.total_size if req.HasField("total_size") else None,
        #         "is_lab": req.is_lab if req.HasField("is_lab") else None,
        #         "chunk": req.chunk if req.HasField("chunk") else None
        #     }
        pass

    def UpdateAsset(self, request_iterator, context) -> stub.Asset:
        # Example for extracting variables from the stream:
        # for req in request_iterator:
        #     data: dict = {
        #         "asset_id": req.asset_id if req.HasField("asset_id") else None,
        #         "filename": req.filename if req.HasField("filename") else None,
        #         "total_size": req.total_size if req.HasField("total_size") else None,
        #         "chunk": req.chunk if req.HasField("chunk") else None
        #     }
        pass

    def DownloadAsset(self, request, context) -> stub.DownloadAssetResponse:
        data: dict = {
            "asset_id": request.asset_id
        }
        pass

    def DeleteAsset(self, request, context) -> stub.DeleteAssetResponse:
        data: dict = {
            "asset_id": request.asset_id
        }
        pass

    def ListAssets(self, request, context) -> stub.AssetList:
        data: dict = {
            "lab_id": request.lab_id
        }
        pass