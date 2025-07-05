# Import downloaded modules
import grpc

# Import built-in modules
import os
import sys
import logging
sys.path.append(os.path.join(os.path.dirname(__file__), "proto"))

# Import project files
from config import Config
import proto.labs_service_pb2 as labs_service_cf
import proto.labs_service_pb2_grpc as labs_service_cf_grpc
import proto.submissions_service_pb2 as submissions_service_cf
import proto.submissions_service_pb2_grpc as submissions_service_cf_grpc


logger = logging.getLogger("Client")
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

def CreateLab(stub, number):
    logger.info(f"Creating lab with: owner_id=1, title=Lab{number}, abstract=Lab{number}")

    response = stub.CreateLab(labs_service_cf.CreateLabRequest(
        owner_id=1,
        title=f'Lab{number}',
        abstract=f'Lab{number}'
    ))
    logger.info(f"Obtained response:\n{response}")

    return response


def GetLab(stub, lab_id):
    logger.info(f"Fetching lab with ID: {lab_id}")

    response = stub.GetLab(labs_service_cf.GetLabRequest(lab_id=lab_id))

    logger.info(f"Obtained response:\n{response}")

    return response


def GetLabs(stub, page_number, page_size):
    logger.info(f"Fetching labs with page_number={page_number}, page_size={page_size}")

    response = stub.GetLabs(labs_service_cf.GetLabsRequest(
        page_number=page_number,
        page_size=page_size
    ))

    logger.info(f"Obtained response:\n{response}")

    return response


def UpdateLab(stub, lab_id, title=None, abstract=None):
    logger.info(f"Updating lab with ID: {lab_id}, title={title}, abstract={abstract}")
    update_lab_request = labs_service_cf.UpdateLabRequest(lab_id=lab_id)

    if title is not None and type(title) is str:
        update_lab_request.title = title
    if abstract is not None and type(abstract) is str:
        update_lab_request.abstract = abstract

    response = stub.UpdateLab(update_lab_request)

    logger.info(f"Obtained response:\n{response}")

    return response


def DeleteLab(stub, lab_id):
    logger.info(f"Deleting lab with ID: {lab_id}")

    response = stub.DeleteLab(labs_service_cf.DeleteLabRequest(lab_id=lab_id))

    logger.info(f"Obtained response:\n{response}")

    return response


def UploadLabAsset(stub, lab_id, filename):
    logger.info(f"Uploading asset for lab ID: {lab_id}, filename: {filename}, filesize: {os.path.getsize(filename)}")

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

    logger.info(f"Obtained response:\n{response}")

    return response


def UpdateLabAsset(stub, asset_id, filename):
    logger.info(f"Updating asset with ID: {asset_id}, filename: {filename}, filesize: {os.path.getsize(filename)}")

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

    logger.info(f"Obtained response:\n{response}")

    return response


def DownloadLabAsset(stub, asset_id):
    logger.info(f"Downloading asset with ID: {asset_id}")

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
        logger.info(f'{asset=}')
    else:
        logger.error("Error: Asset metadata not found.")
        return

    with open(asset["filename"], "wb") as f:
        for response in response_iterator:
            if response.HasField('chunk'):
                if not response.chunk:
                    logger.error("Error: Empty chunk received.")

                f.write(response.chunk)
            else:
                logger.error("Error: Chunk not found in response.")

    logger.info("Donwload completed successfully.")

    return response


def DeleteLabAsset(stub, asset_id):
    logger.info(f"Deleting asset with ID: {asset_id}")

    response = stub.DeleteAsset(labs_service_cf.DeleteAssetRequest(asset_id=asset_id))

    logger.info(f"Obtained response:\n{response}")

    return response


def ListLabAssets(stub, lab_id):
    logger.info(f"Listing assets for lab ID: {lab_id}")

    response = stub.ListAssets(labs_service_cf.ListAssetsRequest(lab_id=lab_id))

    for asset in response.assets:
        logger.info(f"Asset ID: {asset.asset_id}, Filename: {asset.filename}, Filesize: {asset.filesize}, Upload Date: {asset.upload_date}")

    return response


def CreateSubmission(stub, number, lab_id):
    logger.info(f"Creating submission for lab ID: {lab_id}, owner_id=1, text=Submission{number}")

    response = stub.CreateSubmission(submissions_service_cf.CreateSubmissionRequest(
        lab_id=lab_id,
        owner_id=1,
        text=f"Submission{number}"
    ))

    logger.info(f"Obtained response:\n{response}")

    return response


def GetSubmission(stub, submission_id):
    logger.info(f"Fetching submission with ID: {submission_id}")

    response = stub.GetSubmission(submissions_service_cf.GetSubmissionRequest(submission_id=submission_id))

    logger.info(f"Obtained response:\n{response}")

    return response


def GetSubmissions(stub, lab_id, page_number, page_size):
    logger.info(f"Fetching submissions for lab ID: {lab_id}, page_number={page_number}, page_size={page_size}")

    response = stub.GetSubmissions(submissions_service_cf.GetSubmissionsRequest(
        lab_id=lab_id,
        page_number=page_number,
        page_size=page_size
    ))

    logger.info(f"Obtained response:\n{response}")

    return response


def UpdateSubmission(stub, submission_id, status=None, text=None):
    logger.info(f"Updating submission with ID: {submission_id}, status={status}, text={text}")

    request = submissions_service_cf.UpdateSubmissionRequest(submission_id=submission_id)

    if status is not None:
        request.status = status
    if text is not None:
        request.text = text

    response = stub.UpdateSubmission(request)

    logger.info(f"Obtained response:\n{response}")

    return response


def DeleteSubmission(stub, submission_id):
    logger.info(f"Deleting submission with ID: {submission_id}")

    response = stub.DeleteSubmission(submissions_service_cf.DeleteSubmissionRequest(submission_id=submission_id))

    logger.info(f"Obtained response:\n{response}")

    return response


def UploadSubmissionAsset(stub, submission_id, filename):
    logger.info(f"Uploading asset for submission ID: {submission_id}, filename: {filename}, filesize: {os.path.getsize(filename)}")

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

    logger.info(f"Obtained response:\n{response}")

    return response


def UpdateSubmissionAsset(stub, asset_id, filename):
    logger.info(f"Updating asset with ID: {asset_id}, filename: {filename}, filesize: {os.path.getsize(filename)}")

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

    logger.info(f"Obtained response:\n{response}")

    return response


def DownloadSubmissionAsset(stub, asset_id):
    logger.info(f"Downloading asset with ID: {asset_id}")

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
        logger.info(f'Obtained {asset=}')
    else:
        logger.error("Error: Asset metadata not found.")
        return

    with open(asset["filename"], "wb") as f:
        for response in response_iterator:
            if response.HasField('chunk'):
                if not response.chunk:
                    logger.error("Error: Empty chunk received.")

                f.write(response.chunk)
            else:
                logger.error("Error: Chunk not found in response.")

    logger.info("Download completed successfully.")

    return response


def DeleteSubmissionAsset(stub, asset_id):
    logger.info(f"Deleting asset with ID: {asset_id}")

    response = stub.DeleteAsset(submissions_service_cf.DeleteAssetRequest(asset_id=asset_id))

    logger.info(f"Obtained response:\n{response}")

    return response


def ListSubmissionAssets(stub, submission_id):
    logger.info(f"Listing assets for submission ID: {submission_id}")
    response = stub.ListAssets(submissions_service_cf.ListAssetsRequest(submission_id=submission_id))

    for asset in response.assets:
        logger.info(f"Asset ID: {asset.asset_id}, Filename: {asset.filename}, Filesize: {asset.filesize}, Upload Date: {asset.upload_date}")

    return response


def main(server_address: str):
    with grpc.insecure_channel(server_address) as channel:
        labs_service_stub = labs_service_cf_grpc.LabServiceStub(channel)
        submissions_service_stub = submissions_service_cf_grpc.SubmissionServiceStub(channel)

        # CreateLab(labs_service_stub, number=2)

        # GetLab(labs_service_stub, lab_id=1)

        # GetLabs(labs_service_stub, page_number=1, page_size=1000)

        # UpdateLab(labs_service_stub, lab_id=1, title='Updated Lab', abstract='Updated Abstract')

        # DeleteLab(labs_service_stub, lab_id=1)

        # UploadLabAsset(labs_service_stub, lab_id=2, filename='image.jpg')

        # UpdateLabAsset(labs_service_stub, asset_id=1, filename='logo.png')

        # DownloadLabAsset(labs_service_stub, asset_id=1)

        # DeleteLabAsset(labs_service_stub, asset_id=1)

        # ListLabAssets(labs_service_stub, lab_id=2)

        # CreateSubmission(submissions_service_stub, 2, 2)

        # GetSubmission(submissions_service_stub, submission_id=1)

        # GetSubmissions(submissions_service_stub, 2, 1, 1000)

        # UpdateSubmission(submissions_service_stub, 1, status=submissions_service_cf.Status.IN_PROGRESS)

        # DeleteSubmission(submissions_service_stub, 1)

        # UploadSubmissionAsset(submissions_service_stub, 2, 'image.jpg')

        # UpdateSubmissionAsset(submissions_service_stub, 1, 'logo.png')

        # DownloadSubmissionAsset(submissions_service_stub, 1)

        # DeleteSubmissionAsset(submissions_service_stub, 1)

        # ListSubmissionAssets(submissions_service_stub, 2)

if __name__ == "__main__":
    address = f"{Config.SERVICE_HOST}:{Config.SERVICE_PORT}"
    logger.info(f"Starting gRPC server on {address}")

    try:
        main(address)
    except grpc.RpcError as e:
        logger.error(f"gRPC error (code={e.code()}): {e.details()}")
    except KeyboardInterrupt:
        logger.info("Exiting...")