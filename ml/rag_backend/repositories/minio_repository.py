from minio import Minio
from minio.error import S3Error
import typing as tp
import os
import io


class MinioRepository:
    def __init__(self):
        self.client = Minio(
            endpoint="minio:9000",
            access_key=os.getenv("MINIO_ROOT_USER"),
            secret_key=os.getenv("MINIO_ROOT_PASSWORD"),
            secure=False
        )

    def list_files(self, bucket_name: str, prefix: tp.Optional[str] = None) -> tp.List[str | None]:
        objects = self.client.list_objects(bucket_name, prefix=prefix, recursive=True)
        return [obj.object_name for obj in objects]
    

    def get_assignment(self, assignment_id: str) -> tp.List[tp.Tuple[str, bytes]] | None:
        bucket_name = "labs"

        files = self.list_files(bucket_name, prefix=assignment_id)
        if not files:
            return None

        result = []
        for object_name in files:
            if object_name.endswith(".md"):
                data = self._get_file(bucket_name, object_name)
                result.append((object_name, data))
        return result


    
    def get_submission(self, submission_id: str) -> tp.List[tp.Tuple[str, bytes]] | None:
        bucket_name = "submissions"
        extensions = [
            ".py", ".java", ".js", ".ts", ".cpp", ".c", ".h", ".hpp", ".cs", ".go", ".rb", ".php", ".swift",
            ".kt", ".scala", ".rs", ".m", ".sh", ".bat", ".pl", ".lua", ".dart", ".html", ".css", ".json", ".xml",
            ".yaml", ".yml", ".sql", ".dockerfile", "Dockerfile", ".env", ".ini", ".cfg", ".conf", ".toml",
            ".rst", ".ipynb", ".ps1", ".vb", ".asp", ".jsp", ".tsx", ".jsx", ".groovy", ".gradle",
            ".make", "Makefile", ".cmake"
        ]

        files = self.list_files(bucket_name, prefix=submission_id)
        if not files:
            return None

        result = []
        for object_name in files:
            if any(object_name.endswith(ext) for ext in extensions):
                data = self._get_file(bucket_name, object_name)
                result.append((object_name, data))
        return result

    def _get_file(self, bucket_name: str, object_name: str) -> bytes:
        try:
            response = self.client.get_object(bucket_name, object_name)
            data = response.read()
            response.close()
            response.release_conn()
            return data
        except S3Error as e:
            # Handle error as needed
            raise e
        
    def upload_files(self, bucket: str, id: str, dir_path: str, content_type: str = "application/octet-stream"):
        found = self.client.bucket_exists(bucket)
        if not found:
            self.client.make_bucket(bucket)
        
        for file_name in os.listdir(dir_path):
            file_path = os.path.join(dir_path, file_name)
            if os.path.isfile(file_path):
                object_name = f"{id}/{file_name}"
                with open(file_path, "rb") as f:
                    file_data = f.read()
                try:
                    self.client.put_object(
                        bucket_name=bucket,
                        object_name=object_name,
                        data=io.BytesIO(file_data),
                        length=len(file_data),
                        content_type=content_type
                    )
                except S3Error as e:
                    raise e