import pytest
import grpc
import sys
import os

# Fixes import path for proto files
sys.path.append(os.path.join(os.path.dirname(__file__), "proto"))

# Import project files
from config import Config
import client
import proto.articles_service_pb2 as stub
import proto.articles_service_pb2_grpc as service


ADDRESS = f"{Config.SERVICE_HOST}:{Config.SERVICE_PORT}"
CREATE_ARTICLE = False
DELETE_ARTICLE = False
ARTICLE_ID = None

def test_create_article():
    """Test CreateArticle method"""
    global ADDRESS, CREATE_ARTICLE, ARTICLE_ID
    number = 1
    with grpc.insecure_channel(ADDRESS) as channel:
        article_service = service.ArticleServiceStub(channel)
        
        create_article_response = client.CreateArticle(article_service, number)

        expected = {
            "owner_id": 1,
            "title": f"Article{number}",
            "abstract": f"Article{number}",
            "views": 0,
            "stars": 0,
            "people_rated": 0
        }

        for key, value in expected.items():
            assert hasattr(create_article_response, key), f"Response doesn't has '{key}' attribute!"
            assert getattr(create_article_response, key) == value, f"Response '{key}' attribute not as expected ({value})!"
        
        ARTICLE_ID = create_article_response.article_id
        CREATE_ARTICLE = True

def test_delete_article():
    """Test DeleteArticle method"""
    global ADDRESS, DELETE_ARTICLE, ARTICLE_ID

    assert ARTICLE_ID is not None, "Article id is unknown"

    with grpc.insecure_channel(ADDRESS) as channel:
        article_service = service.ArticleServiceStub(channel)
        response = client.DeleteArticle(article_service, ARTICLE_ID)
        
        expected = {
            "success": True
        }
        
        for key, value in expected.items():
            assert hasattr(response, key), f"Response doesn't has '{key}' attribute!"
            assert getattr(response, key) == value, f"Response '{key}' attribute not as expected ({value})!"

        DELETE_ARTICLE = True

def test_get_article():
    """Test GetArticle method"""
    global ADDRESS, CREATE_ARTICLE, DELETE_ARTICLE
    
    assert CREATE_ARTICLE and DELETE_ARTICLE, "Not available until CreateArticle and DeleteArticle are working properly"

    number = 1
    with grpc.insecure_channel(ADDRESS) as channel:
        article_service = service.ArticleServiceStub(channel)
        create_article_response = client.CreateArticle(article_service, number)
        get_article_response = client.GetArticle(article_service, create_article_response.article_id)
        delete_article_response = client.DeleteArticle(article_service, create_article_response.article_id)
        
        expected = {
            "article_id": create_article_response.article_id,
            "owner_id": create_article_response.owner_id,
            "title": create_article_response.title,
            "created_at": create_article_response.created_at,
            "updated_at": create_article_response.updated_at,
            "abstract": create_article_response.abstract,
            "views": create_article_response.views,
            "stars": create_article_response.stars,
            "people_rated": create_article_response.people_rated
        }

        for key, value in expected.items():
            assert hasattr(get_article_response, key), f"Response doesn't has '{key}' attribute!"
            assert getattr(get_article_response, key) == value, f"Response '{key}' attribute not as expected ({value})!"

def test_get_articles():
    """Test GetArticles method"""
    global ADDRESS, CREATE_ARTICLE, DELETE_ARTICLE

    assert CREATE_ARTICLE and DELETE_ARTICLE, "Not available until CreateArticle and DeleteArticle are working properly"
    
    number = 1
    page_number = 1
    page_size = 10
    text = ""
    tags_ids = []

    with grpc.insecure_channel(ADDRESS) as channel:
        article_service = service.ArticleServiceStub(channel)
        create_article_response = client.CreateArticle(article_service, number)
        get_articles_response = client.GetArticles(article_service, page_number, page_size, text, tags_ids)
        delete_article_response = client.DeleteArticle(article_service, create_article_response.article_id)

        assert hasattr(get_articles_response, "total_count"), "Response doesn't has 'total_count' attribute!"
        assert hasattr(get_articles_response, "articles"), "Response doesn't has 'articles' attribute!"
        assert 1 <= getattr(get_articles_response, "total_count") <= page_size, f"Response 'total_count' attribute doesn't in expected bounds (1, {page_size})"
        assert len(getattr(get_articles_response, "articles")) == getattr(get_articles_response, "total_count"), f"Response 'articles' attribute length doesn't match its 'total_count' ({getattr(get_articles_response, "total_count")})!"

def test_get_articles_by_user_id():
    """Test GetArticlesByUserId method"""
    global ADDRESS, CREATE_ARTICLE, DELETE_ARTICLE
    
    assert CREATE_ARTICLE and DELETE_ARTICLE, "Not available until CreateArticle and DeleteArticle are working properly"
    
    number = 1
    page_number = 1
    page_size = 10

    with grpc.insecure_channel(ADDRESS) as channel:
        article_service = service.ArticleServiceStub(channel)
        create_article_response = client.CreateArticle(article_service, number)
        get_articles_by_user_id_response = client.GetArticlesByUserId(article_service, create_article_response.owner_id, page_number, page_size)
        delete_article_response = client.DeleteArticle(article_service, create_article_response.article_id)
        
        assert hasattr(get_articles_by_user_id_response, "total_count"), "Response doesn't has 'total_count' attribute!"
        assert hasattr(get_articles_by_user_id_response, "articles"), "Response doesn't has 'articles' attribute!"
        assert 1 <= getattr(get_articles_by_user_id_response, "total_count") <= page_size, f"Response 'total_count' attribute doesn't in expected bounds (1, {page_size})"
        assert len(getattr(get_articles_by_user_id_response, "articles")) == getattr(get_articles_by_user_id_response, "total_count"), f"Response 'articles' attribute length doesn't match its 'total_count' ({getattr(get_articles_by_user_id_response, "total_count")})!"

        for article in getattr(get_articles_by_user_id_response, "articles"):
            assert article.owner_id == create_article_response.owner_id, f"Article owner_id doesn't match the created article's owner_id ({create_article_response.owner_id})!"

def test_update_article():
    """Test UpdateArticle method"""
    global ADDRESS, CREATE_ARTICLE, DELETE_ARTICLE
    
    assert CREATE_ARTICLE and DELETE_ARTICLE, "Not available until CreateArticle and DeleteArticle are working properly"

    number = 1
    title = "Updated Article"
    abstract = "Updated Article"

    with grpc.insecure_channel(ADDRESS) as channel:
        article_service = service.ArticleServiceStub(channel)
        create_article_response = client.CreateArticle(article_service, number)
        update_article_response = client.UpdateArticle(article_service, create_article_response.article_id, title, abstract)
        delete_article_response = client.DeleteArticle(article_service, create_article_response.article_id)
        
        expected = {
            "article_id": create_article_response.article_id,
            "owner_id": create_article_response.owner_id,
            "title": title,
            "abstract": abstract
        }

        for key, value in expected.items():
            assert hasattr(update_article_response, key), f"Response doesn't has '{key}' attribute!"
            assert getattr(update_article_response, key) == value, f"Response '{key}' attribute not as expected ({value})!"
