import grpc
import proto.articles_pb2 as cf
import proto.articles_pb2_grpc as cf_grpc

from config import Config
import os


def CreateArticle(stub, number):
    pass


def GetArticle(stub, article_id):
    pass


def GetArticles(stub, page_number, page_size):
    pass


def UpdateArticle(stub, article_id, title, abstract):
    pass


def DeleteArticle(stub, article_id):
    pass


def UploadAsset(stub, article_id, filename):
    pass


def UpdateAsset(stub, asset_id, filename):
    pass


def DownloadAsset(stub, asset_id):
    pass


def DeleteAsset(stub, asset_id):
    pass


def ListAssets(stub, article_id):
    pass


def main(server_address: str):
    with grpc.insecure_channel(server_address) as channel:
        stub = cf_grpc.ArticleServiceStub(channel)

        # CreateArticle(stub, number=3)

        # GetArticle(stub, article_id=1)

        # GetArticles(stub, page_number=1, page_size=1000)

        # UpdateArticle(stub, article_id=1, title='Updated Article', abstract='Updated Abstract')

        # DeleteArticle(stub, article_id=1)

        # UploadAsset(stub, article_id=2, filename='image.jpg')

        # UpdateAsset(stub, asset_id=4, filename='logo.png')

        # DownloadAsset(stub, asset_id=5)

        # DeleteAsset(stub, asset_id=4)

        # ListAssets(stub, article_id=2)


if __name__ == "__main__":
    address = f"{Config.SERVICE_HOST}:{Config.SERVICE_PORT}"
    print(address)

    try:
        main(address)
    except grpc.RpcError as e:
        print(f"gRPC error (code={e.code()}): {e.details()}")
    except KeyboardInterrupt:
        print("Exiting...")