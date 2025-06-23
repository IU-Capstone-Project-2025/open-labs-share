# Import downloaded modules
import grpc

# Import built-in modules

# Import project files
from config import Config
import proto.labs_pb2 as stub # Generated from labs.proto
import proto.labs_pb2_grpc as service # Generated from labs.proto

class LabService(service.LabServiceServicer):
    def __init__(self):
        pass

    # Labs Management
    def CreateLab(self, request, context) -> stub.Lab:
        data: dict = {
            "owner_id": request.owner_id,
            "title": request.title,
            "abstract": request.abstract,
            "related_articles": request.related_articles if hasattr(request, "related_articles") else None
        }

        return stub.Lab()

    def GetLab(self, request, context) -> stub.Lab:
        data: dict = {
            "lab_id": request.lab_id
        }
        pass

    def GetLabs(self, request, context) -> stub.LabList:
        data: dict = {
            "page_number": request.page_number,
            "page_size": request.page_size
        }
        pass

    def UpdateLab(self, request, context) -> stub.Lab:
        data: dict = {
            "lab_id": request.lab_id,
            "title": request.title if hasattr(request, "title") else None,
            "abstract": request.abstract if hasattr(request, "abstract") else None,
            "related_articles": request.related_articles if hasattr(request, "related_articles") else None
        }
        pass

    def DeleteLab(self, request, context) -> stub.DeleteLabResponse:
        data: dict = {
            "lab_id": request.lab_id
        }
        pass

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