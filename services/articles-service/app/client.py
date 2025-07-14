# Import downloaded modules
import grpc

# Import built-in modules
import sys
import os
import logging

# Fixes import path for proto files
sys.path.append(os.path.join(os.path.dirname(__file__), "proto"))

# Import project files
from config import Config
import proto.articles_service_pb2 as cf
import proto.articles_service_pb2_grpc as cf_grpc

def CreateArticle(stub, number):
    response = stub.CreateArticle(cf.CreateArticleRequest(
        owner_id=1,
        title=f"Article{number}",
        abstract=f"Article{number}"
    ))
    return response


def GetArticle(stub, article_id):
    response = stub.GetArticle(cf.GetArticleRequest(
        article_id=article_id
    ))
    return response


def GetArticles(stub, page_number, page_size, text, tags_ids):
    response = stub.GetArticles(cf.GetArticlesRequest(
        page_number=page_number,
        page_size=page_size,
        tags_ids=tags_ids
    ))

    if text:
        response.text = text
        
    return response


def GetArticlesByUserId(stub, user_id, page_number, page_size):
    response = stub.GetArticlesByUserId(cf.GetArticlesByUserIdRequest(
        user_id=user_id,
        page_number=page_number,
        page_size=page_size
    ))
    return response


def UpdateArticle(stub, article_id, title=None, abstract=None):
    update_article = cf.UpdateArticleRequest(article_id=article_id)

    if title:
        update_article.title = title

    if abstract:
        update_article.abstract = abstract

    response = stub.UpdateArticle(update_article)

    return response


def DeleteArticle(stub, article_id):
    response = stub.DeleteArticle(cf.DeleteArticleRequest(
        article_id=article_id
    ))
    return response


def UploadAsset(stub, article_id, filename):
    def generate_requests():
        yield cf.UploadAssetRequest(metadata=cf.UploadAssetMetadata(
            article_id=article_id,
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
            "article_id": asset_metadata.asset.article_id,
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


def ListAssets(stub, article_id):
    response = stub.ListAssets(cf.ListAssetsRequest(
        article_id=article_id
    ))
    print(response)
    for asset in response.assets:
        print(f"Asset ID: {asset.asset_id}, Article ID: {asset.article_id}, Filename: {asset.filename}, Filesize: {asset.filesize}")


def main(server_address: str):
    with grpc.insecure_channel(server_address) as channel:
        stub = cf_grpc.ArticleServiceStub(channel)

        # CreateArticle(stub, number=1)

        # GetArticle(stub, article_id=2)

        # GetArticles(stub, page_number=1, page_size=1000)

        # UpdateArticle(stub, article_id=2, title='Updated Article', abstract='Updated Abstract')

        # DeleteArticle(stub, article_id=2)

        # UploadAsset(stub, article_id=1, filename='OLS_BM_PDF.pdf')

        # UpdateAsset(stub, asset_id=1, filename='Open Share Labs.pdf')

        # DownloadAsset(stub, asset_id=1)

        # DeleteAsset(stub, asset_id=1)

        # ListAssets(stub, article_id=1)


if __name__ == "__main__":
    address = f"{Config.SERVICE_HOST}:{Config.SERVICE_PORT}"
    print(address)

    try:
        main(address)
    except grpc.RpcError as e:
        print(f"gRPC error (code={e.code()}): {e.details()}")
    except KeyboardInterrupt:
        print("Exiting...")