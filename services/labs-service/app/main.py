# Import downloaded modules
import grpc
from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session
import minio

# Import built-in modules
import os
from concurrent import futures

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

        self.engine = create_engine(url, echo=False)
        
        # Create tables if they don't exist
        from utils.models import Base
        Base.metadata.create_all(self.engine)
        
        self.minio_client = minio.Minio(
            endpoint=Config.MINIO_ENDPOINT,
            access_key=Config.MINIO_ACCESS_KEY,
            secret_key=Config.MINIO_SECRET_KEY,
            secure=False
        )

        if not self.minio_client.bucket_exists("labs"):
            self.minio_client.make_bucket("labs")

        # Ensure the temporary files directory exists
        if not os.path.exists('files'):
            os.makedirs('files')

    # Labs Management
    def CreateLab(self, request, context) -> stub.Lab:
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
        page_number = request.page_number if request.page_number > 0 else 1
        page_size = request.page_size

        with Session(self.engine) as session:
            # Get total count of labs
            total_count = session.query(Lab).count()

            # Get paginated labs
            stmt = select(Lab).offset((page_number - 1) * page_size).limit(page_size)
            labs = session.execute(stmt).scalars().all()

            lab_list = stub.LabList(total_count=total_count)
            for lab in labs:
                lab_list.labs.append(stub.Lab(**lab.get_attrs()))

            return lab_list


    def UpdateLab(self, request, context) -> stub.Lab:
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
            return stub.Asset()

        with Session(self.engine) as session:
            # Check if lab exists
            stmt = select(Lab).where(Lab.id == data["lab_id"])
            lab = session.execute(stmt).scalar_one_or_none()

            if lab is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Lab not found")
                return stub.Asset()

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
                            return stub.Asset()

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
                return stub.Asset()

            session.commit()
            return stub.Asset(**new_asset.get_attrs())


    def UpdateAsset(self, request_iterator, context) -> stub.Asset:
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
            return stub.Asset()

        with Session(self.engine) as session:
            # Check if lab exists
            stmt = select(LabAsset).where(LabAsset.id == data["asset_id"])
            lab_asset = session.execute(stmt).scalar_one_or_none()

            if lab_asset is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Asset not found")
                return stub.Asset()

            # Try to remove the old asset file from MinIO
            try:
                self.minio_client.remove_object('labs', f"{lab_asset.lab_id}/{lab_asset.filename}")
            except Exception as e:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details(f"Failed to delete asset from MinIO: {str(e)}")
                return stub.Asset()

            lab_asset.filename = data["filename"]
            lab_asset.filesize = data["filesize"]


            with open(f'files/{lab_asset.filename}', 'wb') as f:
                for request in request_iterator:
                    if request.HasField('chunk'):
                        f.write(request.chunk)
                    else:
                        context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                        context.set_details("Subsequent requests must contain chunk data")
                        return stub.Asset()

            # Put the file in MinIO
            self.minio_client.fput_object(
                "labs",
                f"{lab_asset.lab_id}/{lab_asset.filename}",
                f'files/{lab_asset.filename}'
            )

            # Clean up local file after upload
            os.remove(f'files/{lab_asset.filename}')

            session.commit()
            return stub.Asset(**lab_asset.get_attrs())


    def DownloadAsset(self, request, context) -> stub.DownloadAssetResponse:
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
                    return stub.DownloadAssetResponse()

                yield stub.DownloadAssetResponse(asset=stub.Asset(**lab_asset.get_attrs()))

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
                    return stub.DownloadAssetResponse()

                with open(f'files/{lab_asset.filename}', 'rb') as f:
                    while True:
                        chunk = f.read(8 * 1024)

                        if not chunk:
                            break

                        yield stub.DownloadAssetResponse(chunk=chunk)

                # Clean up local file after download
                os.remove(f'files/{lab_asset.filename}')

        return response_messages()


    def DeleteAsset(self, request, context) -> stub.DeleteAssetResponse:
        data: dict = {
            "asset_id": request.asset_id
        }

        with Session(self.engine) as session:
            stmt = select(LabAsset).where(LabAsset.id == data["asset_id"])
            asset = session.execute(stmt).scalar_one_or_none()

            if asset is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Asset not found")
                return stub.DeleteAssetResponse(success=False)

            session.delete(asset)

            # Remove the asset from MinIO
            try:
                self.minio_client.remove_object('labs', f"{asset.lab_id}/{asset.filename}")
            except Exception as e:
                context.set_code(grpc.StatusCode.INTERNAL)
                context.set_details(f"Failed to delete asset from MinIO: {str(e)}")
                return stub.DeleteAssetResponse(success=False)

            session.commit()

            return stub.DeleteAssetResponse(success=True)


    def ListAssets(self, request, context) -> stub.AssetList:
        data: dict = {
            "lab_id": request.lab_id
        }

        with Session(self.engine) as session:
            stmt = select(Lab).where(Lab.id == data["lab_id"])
            lab = session.execute(stmt).scalar_one_or_none()

            if lab is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Lab not found")
                return stub.AssetList()

            if not lab.assets:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("Lab has no assets")
                return stub.AssetList()

            asset_list = stub.AssetList()
            asset_list.total_count = len(lab.assets)
            asset_list.assets.extend([stub.Asset(**asset.get_attrs()) for asset in lab.assets])

            return asset_list


if __name__ == "__main__":
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    service.add_LabServiceServicer_to_server(LabService(), server)

    server_address = f"{Config.SERVICE_HOST}:{Config.SERVICE_PORT}"
    server.add_insecure_port(server_address)
    print(f"Starting gRPC server on {server_address}")
    server.start()
    try:
        server.wait_for_termination()
    except KeyboardInterrupt:
        print("Server is shutting down...")
        server.stop(0)