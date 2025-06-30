import sys
import os
sys.path.append(os.path.join(os.path.dirname(__file__), "proto"))

import grpc
import proto.labs_service_pb2 as labs_service_cf
import proto.labs_service_pb2_grpc as labs_service_cf_grpc
import proto.submissions_service_pb2 as submissions_service_cf
import proto.submissions_service_pb2_grpc as submissions_service_cf_grpc

from config import Config


def CreateLab(stub, number):
    response = stub.CreateLab(labs_service_cf.CreateLabRequest(
        owner_id=1,
        title=f'Lab{number}',
        abstract=f'Lab{number}'
    ))
    print(response)


def GetLab(stub, lab_id):
    response = stub.GetLab(labs_service_cf.GetLabRequest(lab_id=lab_id))
    print(response)


def GetLabs(stub, page_number, page_size):
    response = stub.GetLabs(labs_service_cf.GetLabsRequest(
        page_number=page_number,
        page_size=page_size
    ))
    print(response)


def UpdateLab(stub, lab_id, title, abstract):
    response = stub.UpdateLab(labs_service_cf.UpdateLabRequest(
        lab_id=lab_id,
        title=title,
        abstract=abstract
    ))
    print(response)


def DeleteLab(stub, lab_id):
    response = stub.DeleteLab(labs_service_cf.DeleteLabRequest(lab_id=lab_id))
    print(response)


def UploadLabAsset(stub, lab_id, filename):
    def generate_requests():
        yield labs_service_cf.UploadAssetRequest(metadata=labs_service_cf.UploadAssetMetadata(
            lab_id=lab_id,
            filename=filename,
            filesize=os.path.getsize(filename)
        ))

        with open(filename, 'rb') as f:
            while True:
                chunk = f.read(8 * 1024)
                if not chunk:
                    break

                yield labs_service_cf.UploadAssetRequest(chunk=chunk)

    response = stub.UploadAsset(generate_requests())
    print(response)


def UpdateLabAsset(stub, asset_id, filename):
    print(asset_id, filename, os.path.getsize(filename))
    def generate_requests():
        yield labs_service_cf.UpdateAssetRequest(metadata=labs_service_cf.UpdateAssetMetadata(
            asset_id=asset_id,
            filename=filename,
            filesize=os.path.getsize(filename)
        ))

        with open(filename, 'rb') as f:
            while True:
                chunk = f.read(8 * 1024)
                if not chunk:
                    break

                yield labs_service_cf.UpdateAssetRequest(chunk=chunk)

    response = stub.UpdateAsset(generate_requests())
    print(response)


def DownloadLabAsset(stub, asset_id):
    response_iterator = stub.DownloadAsset(labs_service_cf.DownloadAssetRequest(asset_id=asset_id))
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


def DeleteLabAsset(stub, asset_id):
    response = stub.DeleteAsset(labs_service_cf.DeleteAssetRequest(asset_id=asset_id))
    print(response)


def ListLabAssets(stub, lab_id):
    response = stub.ListAssets(labs_service_cf.ListAssetsRequest(lab_id=lab_id))
    print("Assets in Lab ID", lab_id)
    for asset in response.assets:
        print(f"Asset ID: {asset.asset_id}, Filename: {asset.filename}, Filesize: {asset.filesize}, Upload Date: {asset.upload_date}")


def CreateSubmission(stub, number, lab_id):
    response = stub.CreateSubmission(submissions_service_cf.CreateSubmissionRequest(
        lab_id=lab_id,
        owner_id=1,
        text=f"Submission{number}"
    ))
    print(response)


def GetSubmission(stub, submission_id):
    response = stub.GetSubmission(submissions_service_cf.GetSubmissionRequest(submission_id=submission_id))
    print(response)


def GetSubmissions(stub, lab_id, page_number, page_size):
    response = stub.GetSubmissions(submissions_service_cf.GetSubmissionsRequest(
        lab_id=lab_id,
        page_number=page_number,
        page_size=page_size
    ))
    print(response)


def UpdateSubmission(stub, submission_id, status=None, points=None, text=None):
    request = submissions_service_cf.UpdateSubmissionRequest(submission_id=submission_id)

    if status is not None:
        request.status = status
    if points is not None:
        request.points = points
    if text is not None:
        request.text = text

    response = stub.UpdateSubmission(request)
    print(response)


def DeleteSubmission(stub, submission_id):
    response = stub.DeleteSubmission(submissions_service_cf.DeleteSubmissionRequest(submission_id=submission_id))
    print(response)


def UploadSubmissionAsset(stub, submission_id, filename):
    def generate_requests():
        yield submissions_service_cf.UploadAssetRequest(metadata=submissions_service_cf.UploadAssetMetadata(
            submission_id=submission_id,
            filename=filename,
            filesize=os.path.getsize(filename)
        ))

        with open(filename, 'rb') as f:
            while True:
                chunk = f.read(8 * 1024)
                if not chunk:
                    break

                yield submissions_service_cf.UploadAssetRequest(chunk=chunk)

    response = stub.UploadAsset(generate_requests())
    print(response)


def UpdateSubmissionAsset(stub, asset_id, filename):
    def generate_requests():
        yield submissions_service_cf.UpdateAssetRequest(metadata=submissions_service_cf.UpdateAssetMetadata(
            asset_id=asset_id,
            filename=filename,
            filesize=os.path.getsize(filename)
        ))

        with open(filename, 'rb') as f:
            while True:
                chunk = f.read(8 * 1024)
                if not chunk:
                    break

                yield submissions_service_cf.UpdateAssetRequest(chunk=chunk)

    response = stub.UpdateAsset(generate_requests())
    print(response)


def DownloadSubmissionAsset(stub, asset_id):
    response_iterator = stub.DownloadAsset(submissions_service_cf.DownloadAssetRequest(asset_id=asset_id))
    asset_metadata = next(response_iterator)

    if asset_metadata.HasField('asset'):
        asset = {
            "asset_id": asset_metadata.asset.asset_id,
            "submission_id": asset_metadata.asset.submission_id,
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

    print("Download completed successfully.")


def DeleteSubmissionAsset(stub, asset_id):
    response = stub.DeleteAsset(submissions_service_cf.DeleteAssetRequest(asset_id=asset_id))
    print(response)


def ListSubmissionAssets(stub, submission_id):
    response = stub.ListAssets(submissions_service_cf.ListAssetsRequest(submission_id=submission_id))
    print("Assets in Submission ID", submission_id)
    for asset in response.assets:
        print(f"Asset ID: {asset.asset_id}, Filename: {asset.filename}, Filesize: {asset.filesize}, Upload Date: {asset.upload_date}")


def main(server_address: str):
    with grpc.insecure_channel(server_address) as channel:
        labs_service_stub = labs_service_cf_grpc.LabServiceStub(channel)
        submissions_service_stub = submissions_service_cf_grpc.SubmissionServiceStub(channel)

        # CreateLab(labs_service_stub, number=2)

        # GetLab(labs_service_stub, lab_id=1)

        # GetLabs(labs_service_stub, page_number=1, page_size=1000)

        # UpdateLab(labs_service_stub, lab_id=1, title='Updated Lab', abstract='Updated Abstract')

        # DeleteLab(labs_service_stub, lab_id=4)

        # UploadLabAsset(labs_service_stub, lab_id=2, filename='image.jpg')

        # UpdateLabAsset(labs_service_stub, asset_id=1, filename='logo.png')

        # DownloadLabAsset(labs_service_stub, asset_id=1)

        # DeleteLabAsset(labs_service_stub, asset_id=1)

        # ListLabAssets(labs_service_stub, lab_id=2)

        # CreateSubmission(submissions_service_stub, 2, 2)

        # GetSubmission(submissions_service_stub, submission_id=1)

        # GetSubmissions(submissions_service_stub, 2, 1, 1000)

        # UpdateSubmission(submissions_service_stub, 2, points=10)

        # DeleteSubmission(submissions_service_stub, 3)

        # UploadSubmissionAsset(submissions_service_stub, 2, 'image.jpg')

        # UpdateSubmissionAsset(submissions_service_stub, 4, 'logo.png')

        # DownloadSubmissionAsset(submissions_service_stub, 4)

        # DeleteSubmissionAsset(submissions_service_stub, 4)

        # ListSubmissionAssets(submissions_service_stub, 2)

if __name__ == "__main__":
    address = f"{Config.SERVICE_HOST}:{Config.SERVICE_PORT}"
    print(address)

    try:
        main(address)
    except grpc.RpcError as e:
        print(f"gRPC error (code={e.code()}): {e.details()}")
    except KeyboardInterrupt:
        print("Exiting...")