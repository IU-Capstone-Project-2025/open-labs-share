from pydantic import BaseModel

class AutoGradingRequest(BaseModel):
    uuid: str
    assignment_id: str
    submission_id: str
    webhook_url: str 
    # Example: "http://name-of-docker-service:internal-port-of-docker-service/"