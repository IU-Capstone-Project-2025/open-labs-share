# Import downloaded modules
import grpc
import pytest

# Import built-in modules
import os
import sys
import logging
sys.path.append(os.path.join(os.path.dirname(__file__), "proto"))

# Import project files
from config import Config
import client
import proto.labs_service_pb2 as labs_service_cf
import proto.labs_service_pb2_grpc as labs_service_cf_grpc
import proto.submissions_service_pb2 as submissions_service_cf
import proto.submissions_service_pb2_grpc as submissions_service_cf_grpc

logger = logging.getLogger("Tester")
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

# with grpc.insecure_channel(ADDRESS) as channel:
#     labs_service_stub = labs_service_cf_grpc.LabServiceStub(channel)
#     submission_service_stub = submissions_service_cf_grpc.SubmissionServiceStub(channel)

ADDRESS = f"{Config.SERVICE_HOST}: {Config.SERVICE_PORT}"

CREATE_LAB = False
DELETE_LAB = False
LAB_ID = None

CREATE_SUBMISSION = False
DELETE_SUBMISSION = False
SUB_LAB_ID = None
SUBMISSION_ID = None

def test_CreateLab():
    global ADDRESS, LAB_ID, CREATE_LAB

    number = 1

    with grpc.insecure_channel(ADDRESS) as channel:
        labs_service_stub = labs_service_cf_grpc.LabServiceStub(channel)

        response = client.CreateLab(labs_service_stub, number)

        expected = {
            "owner_id": 1,
            "title": f"Lab{number}",
            "abstract": f"Lab{number}",
            "views": 0,
            "submissions": 0,
            "stars_total": 0,
            "people_rated": 0,
            "related_articles": labs_service_cf.ArticleList(total_count=0)
        }

        for key, value in expected.items():
            assert hasattr(response, key), f"Response doesn't has '{key}' attribute!"
            assert getattr(response, key) == value, f"Response '{key}' attribute not as expected ({value})!"

        LAB_ID = response.lab_id
        CREATE_LAB = True


def test_DeleteLab():
    global ADDRESS, LAB_ID, DELETE_LAB

    if LAB_ID is None:
        assert False, "Lab id is unknown"

    with grpc.insecure_channel(ADDRESS) as channel:
        labs_service_stub = labs_service_cf_grpc.LabServiceStub(channel)
        response = client.DeleteLab(labs_service_stub, LAB_ID)

        expected = {
            "success": True
        }

        for key, value in expected.items():
            assert hasattr(response, key), f"Response doesn't has {key} attribute!"
            assert getattr(response, key) == value, f"Response '{key}' attribute not as expected ({value})!"

        DELETE_LAB = True


def test_GetLab():
    global ADDRESS, CREATE_LAB, DELETE_LAB
    number = 1

    if (not CREATE_LAB) or (not DELETE_LAB):
        assert False, f"Not available until CreateLab={CREATE_LAB} and DeleteLab={DELETE_LAB} are working properly"

    with grpc.insecure_channel(ADDRESS) as channel:
        labs_service_stub = labs_service_cf_grpc.LabServiceStub(channel)

        new_lab = client.CreateLab(labs_service_stub, 1)
        got_lab = client.GetLab(labs_service_stub, new_lab.lab_id)
        delete_lab = client.DeleteLab(labs_service_stub, new_lab.lab_id)

        assert new_lab == got_lab, "Created Lab doesn't match retrieved lab"


def test_GetLabs():
    global ADDRESS, CREATE_LAB, DELETE_LAB
    page_number = 1
    page_size = 100

    if (not CREATE_LAB) or (not DELETE_LAB):
        assert False, f"Not available until CreateLab={CREATE_LAB} and DeleteLab={DELETE_LAB} are working properly"

    with grpc.insecure_channel(ADDRESS) as channel:
        labs_service_stub = labs_service_cf_grpc.LabServiceStub(channel)
        new_lab = client.CreateLab(labs_service_stub, 1)
        response = client.GetLabs(labs_service_stub, page_number, page_size)
        delete_lab = client.DeleteLab(labs_service_stub, new_lab.lab_id)

        assert hasattr(response, "total_count"), "Response doesn't has 'total_count' attribute!"
        assert hasattr(response, "labs"), "Response doesn't has 'labs' attribute!"
        assert 1 <= getattr(response, "total_count") <= page_size, f"Response 'total_count' attribute doesn't in expected bounds (1, {page_size})"
        assert len(getattr(response, "labs")) == getattr(response, "total_count"), f"Response 'labs' attribute length doesn't match its 'total_count' ({getattr(response, "total_count")})!"


def test_UpdateLab():
    global ADDRESS, CREATE_LAB, DELETE_LAB
    number = 1
    updated_title = "Updated Title" # or None to not change
    updated_abstract = "Updated Abstract" # or None to not change

    assert CREATE_LAB == DELETE_LAB == True, f"Not available until CreateLab={CREATE_LAB} and DeleteLab={DELETE_LAB} are working properly"

    with grpc.insecure_channel(ADDRESS) as channel:
        labs_service_stub = labs_service_cf_grpc.LabServiceStub(channel)
        new_lab = client.CreateLab(labs_service_stub, number)
        updated_lab = client.UpdateLab(labs_service_stub, new_lab.lab_id, updated_title, updated_abstract)
        delete_lab = client.DeleteLab(labs_service_stub, new_lab.lab_id)

        lab_common_attrs = ["lab_id", "owner_id", "created_at", "views", "submissions", "stars_total", "people_rated", "related_articles"]

        for attr in lab_common_attrs:
            # Assume new_lab obj is fine
            assert hasattr(updated_lab, attr), f"Updated Lab doesn't has '{attr}' attribute!"
            assert getattr(new_lab, attr) == getattr(updated_lab, attr), f"Updated Lab attribute '{attr}' differs not as expected!"

        if updated_title is not None and type(updated_title) is str:
            assert getattr(updated_lab, "title") == updated_title, f"Updated Lab 'title' attribute not as expected ({updated_title})!"

        if updated_abstract is not None and type(updated_abstract) is str:
            assert getattr(updated_lab, "abstract") == updated_abstract, f"Updated Lab 'abstract' attribute not as expected ({updated_abstract})!"


def test_CreateSubmission():
    global ADDRESS, CREATE_LAB, CREATE_SUBMISSION, SUB_LAB_ID, SUBMISSION_ID
    number = 1

    assert CREATE_LAB, f"Not available until CreateLab={CREATE_LAB} is working properly"

    with grpc.insecure_channel(ADDRESS) as channel:
        labs_service_stub = labs_service_cf_grpc.LabServiceStub(channel)
        submissions_service_stub = submissions_service_cf_grpc.SubmissionServiceStub(channel)

        new_lab = client.CreateLab(labs_service_stub, number)
        new_submission = client.CreateSubmission(submissions_service_stub, number, new_lab.lab_id)

        expected = {
            "lab_id": new_lab.lab_id,
            "owner_id": 1,
            "text": f"Submission{number}",
            "status": submissions_service_cf.Status.NOT_GRADED
        }

        for key, value in expected.items():
            assert hasattr(new_submission, key), f"Response doesn't has '{key}' attribute!"
            assert getattr(new_submission, key) == value, f"Response '{key}' attribute not as expected ({value})!"

        CREATE_SUBMISSION = True
        SUB_LAB_ID = new_lab.lab_id
        SUBMISSION_ID = new_submission.submission_id


def test_DeleteSubmission():
    global ADDRESS, SUB_LAB_ID, SUBMISSION_ID, DELETE_SUBMISSION, DELETE_LAB

    assert DELETE_LAB, f"Not available until DeleteLab={DELETE_LAB} is working properly"

    assert SUB_LAB_ID is not None, f"Submission lab id is unknown!"
    assert SUBMISSION_ID is not None, f"Submission id is unknown!"

    with grpc.insecure_channel(ADDRESS) as channel:
        labs_service_stub = labs_service_cf_grpc.LabServiceStub(channel)
        submissions_service_stub = submissions_service_cf_grpc.SubmissionServiceStub(channel)

        response = client.DeleteSubmission(submissions_service_stub, SUBMISSION_ID)
        client.DeleteLab(labs_service_stub, SUB_LAB_ID)

        expected = {
            "success": True
        }

        for key, value in expected.items():
            assert hasattr(response, key), f"Response doesn't has '{key}' attribute!"
            assert getattr(response, key) == value, f"Response '{key}' attribute not as expected ({value})!"

    DELETE_SUBMISSION = True



def test_GetSubmission():
    global ADDRESS, CREATE_SUBMISSION, DELETE_SUBMISSION
    number = 1

    assert CREATE_SUBMISSION and DELETE_SUBMISSION, f"Not available until CreateSubmission={CREATE_SUBMISSION} and DeleteSubmission={DELETE_SUBMISSION} are working properly"

    with grpc.insecure_channel(ADDRESS) as channel:
        labs_service_stub = labs_service_cf_grpc.LabServiceStub(channel)
        submissions_service_stub = submissions_service_cf_grpc.SubmissionServiceStub(channel)

        new_lab = client.CreateLab(labs_service_stub, number)
        new_submission = client.CreateSubmission(submissions_service_stub, number, new_lab.lab_id)
        get_submission = client.GetSubmission(submissions_service_stub, new_submission.submission_id)
        delete_submission = client.DeleteSubmission(submissions_service_stub, new_submission.submission_id)
        delete_lab = client.DeleteLab(labs_service_stub, new_lab.lab_id)

        assert new_submission == get_submission, f"Created submission doesn't match retrieved submission!"


def test_GetSubmissions():
    global ADDRESS, CREATE_SUBMISSION, DELETE_SUBMISSION
    number = 1
    page_number = 1
    page_size = 100

    assert CREATE_SUBMISSION and DELETE_SUBMISSION, f"Not available until CreateSubmission={CREATE_SUBMISSION} and DeleteSubmission={DELETE_SUBMISSION} are working properly"

    with grpc.insecure_channel(ADDRESS) as channel:
        labs_service_stub = labs_service_cf_grpc.LabServiceStub(channel)
        submissions_service_stub = submissions_service_cf_grpc.SubmissionServiceStub(channel)

        new_lab = client.CreateLab(labs_service_stub, number)
        new_submission = client.CreateSubmission(submissions_service_stub, number, new_lab.lab_id)
        get_submissions = client.GetSubmissions(submissions_service_stub, new_lab.lab_id, page_number, page_size)
        delete_submission = client.DeleteSubmission(submissions_service_stub, new_submission.submission_id)
        delete_lab = client.DeleteLab(labs_service_stub, new_lab.lab_id)

        assert hasattr(get_submissions, "total_count"), f"Response doesn't has 'total_count' attribute!"
        assert hasattr(get_submissions, "submissions"), f"Response doesn't has 'submissions' attribute!"
        assert 1 <= getattr(get_submissions, "total_count") <= page_size, f"Response attribute 'total_count' not in expected bounds (1, {page_size})!"
        assert len(getattr(get_submissions, "submissions")) == getattr(get_submissions, "total_count"), f"Response 'submissions' attribute doesn't match its 'total_count' attribute!"


def test_UpdateSubmission():
    global ADDRESS, CREATE_SUBMISSION, DELETE_SUBMISSION
    number = 1
    updated_text = "Updated text"
    updated_status = submissions_service_cf.Status.IN_PROGRESS

    assert CREATE_SUBMISSION and DELETE_SUBMISSION, f"Not available until CreateSubmission={CREATE_SUBMISSION} and DeleteSubmission={DELETE_SUBMISSION} are working properly"

    with grpc.insecure_channel(ADDRESS) as channel:
        labs_service_stub = labs_service_cf_grpc.LabServiceStub(channel)
        submissions_service_stub = submissions_service_cf_grpc.SubmissionServiceStub(channel)

        new_lab = client.CreateLab(labs_service_stub, number)
        new_submission = client.CreateSubmission(submissions_service_stub, number, new_lab.lab_id)
        update_submission = client.UpdateSubmission(submissions_service_stub, new_submission.submission_id, updated_status, updated_text)
        delete_submission = client.DeleteSubmission(submissions_service_stub, new_submission.submission_id)
        delete_lab = client.DeleteLab(labs_service_stub, new_lab.lab_id)

        submissions_common_attrs = ["submission_id", "lab_id", "owner_id", "created_at"]

        for attr in submissions_common_attrs:
            # Admit new submission is fine
            assert hasattr(update_submission, attr), f"Response doesn't has '{attr}' attribute!"
            assert getattr(new_submission, attr) == getattr(update_submission, attr), f"Response '{attr}' attribute doesn't match its original!"

        assert hasattr(update_submission, "text"), f"Response doesn't has 'text' attribute!"
        assert hasattr(update_submission, "status"), f"Response doesn't has 'status' attribute!"
        assert getattr(update_submission, "text") == updated_text, f"Response 'text' attribute doesn't match expected ({updated_text})!"
        assert getattr(update_submission, "status") == updated_status, f"Response 'status' attribute doesn't match expected ({updated_status})!"
