import grpc
import proto.labs_pb2 as cf
import proto.labs_pb2_grpc as cf_grpc

from config import Config
import os


def CreateLab(stub, number):
    response = stub.CreateLab(cf.CreateLabRequest(
        owner_id=1,
        title=f'Lab{number}',
        abstract=f'Lab{number}'
    ))
    print(response)


def GetLab(stub, lab_id):
    response = stub.GetLab(cf.GetLabRequest(lab_id=lab_id))
    print(response)


def GetLabs(stub, page_number, page_size):
    response = stub.GetLabs(cf.GetLabsRequest(
        page_number=page_number,
        page_size=page_size
    ))
    print(response)


def UpdateLab(stub, lab_id, title, abstract):
    response = stub.UpdateLab(cf.UpdateLabRequest(
        lab_id=lab_id,
        title=title,
        abstract=abstract
    ))
    print(response)


def DeleteLab(stub, lab_id):
    response = stub.DeleteLab(cf.DeleteLabRequest(lab_id=lab_id))
    print(response)


def UploadAsset(stub, lab_id, filename):
    def generate_requests():
        yield cf.UploadAssetRequest(metadata=cf.UploadAssetMetadata(
            lab_id=lab_id,
            filename=filename,
            filesize=os.path.getsize(filename)
        ))

        with open(filename, 'rb') as f:
            while True:
                chunk = f.read(8 * 1024)
                if not chunk:
                    break

                yield cf.UploadAssetRequest(chunk=chunk)

    response = stub.UploadAsset(generate_requests())
    print(response)


def UpdateAsset(stub, asset_id, filename):
    print(asset_id, filename, os.path.getsize(filename))
    def generate_requests():
        yield cf.UpdateAssetRequest(metadata=cf.UpdateAssetMetadata(
            asset_id=asset_id,
            filename=filename,
            filesize=os.path.getsize(filename)
        ))

        with open(filename, 'rb') as f:
            while True:
                chunk = f.read(8 * 1024)
                if not chunk:
                    break

                yield cf.UpdateAssetRequest(chunk=chunk)

    response = stub.UpdateAsset(generate_requests())
    print(response)


def DownloadAsset(stub, asset_id):
    response_iterator = stub.DownloadAsset(cf.DownloadAssetRequest(asset_id=asset_id))
    asset_metadata = next(response_iterator)

    if asset_metadata.HasField('asset'):
        asset = {
            "asset_id": asset_metadata.asset.asset_id,
            "lab_id": asset_metadata.asset.lab_id,
            "filename": asset_metadata.asset.filename,
            "filesize": asset_metadata.asset.filesize,
            "upload_date": asset_metadata.asset.upload_date
        }
        print(f'{asset=}')
    else:
        print("Error: Asset metadata not found.")
        return

    with open(asset["filename"], "wb") as f:
        for response in response_iterator:
            if response.HasField('chunk'):
                if not response.chunk:
                    print("Error: Empty chunk received.")

                f.write(response.chunk)
            else:
                print("Error: Chunk not found in response.")

    print("Donwload completed successfully.")


def DeleteAsset(stub, asset_id):
    response = stub.DeleteAsset(cf.DeleteAssetRequest(asset_id=asset_id))
    print(response)


def ListAssets(stub, lab_id):
    response = stub.ListAssets(cf.ListAssetsRequest(lab_id=lab_id))
    print("Assets in Lab ID", lab_id)
    for asset in response.assets:
        print(f"Asset ID: {asset.asset_id}, Filename: {asset.filename}, Filesize: {asset.filesize}, Upload Date: {asset.upload_date}")


def main(server_address: str):
    with grpc.insecure_channel(server_address) as channel:
        stub = cf_grpc.LabServiceStub(channel)

        # CreateLab(stub, number=3)

        # GetLab(stub, lab_id=1)

        # GetLabs(stub, page_number=1, page_size=1000)

        # UpdateLab(stub, lab_id=1, title='Updated Lab', abstract='Updated Abstract')

        # DeleteLab(stub, lab_id=1)

        # UploadAsset(stub, lab_id=2, filename='image.jpg')

        # UpdateAsset(stub, asset_id=4, filename='logo.png')

        DownloadAsset(stub, asset_id=5)

        # DeleteAsset(stub, asset_id=4)

        # ListAssets(stub, lab_id=2)


if __name__ == "__main__":
    address = f"{Config.SERVICE_HOST}:{Config.SERVICE_PORT}"
    print(address)

    try:
        main(address)
    except grpc.RpcError as e:
        print(f"gRPC error (code={e.code()}): {e.details()}")
    except KeyboardInterrupt:
        print("Exiting...")