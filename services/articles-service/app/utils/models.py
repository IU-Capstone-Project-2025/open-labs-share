from datetime import datetime
from sqlalchemy import BigInteger, ForeignKey, String, Text, func, TIMESTAMP
from sqlalchemy.orm import Mapped, relationship, declarative_base, mapped_column
from sqlalchemy_serializer import SerializerMixin

Base = declarative_base()


class Article(Base, SerializerMixin):
    __tablename__ = 'articles'

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    owner_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    created_at: Mapped[datetime] = mapped_column(TIMESTAMP(timezone=True), nullable=False, server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(TIMESTAMP(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now())
    abstract: Mapped[str] = mapped_column(Text)
    views: Mapped[int] = mapped_column(BigInteger, default=0)
    stars: Mapped[int] = mapped_column(BigInteger, default=0)
    people_rated: Mapped[int] = mapped_column(BigInteger, default=0)

    assets = relationship("ArticleAsset", back_populates="article", cascade="all, delete")

    def __repr__(self):
        return f"<Article(id={self.id}, title='{self.title}')>"

    def get_attrs(self):
        return {
            "article_id": self.id,
            "owner_id": self.owner_id,
            "title": self.title,
            "created_at": self.created_at,
            "updated_at": self.updated_at,
            "abstract": self.abstract,
            "views": self.views,
            "stars": self.stars,
            "people_rated": self.people_rated
        }


class ArticleAsset(Base, SerializerMixin):
    __tablename__ = 'article_assets'

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    article_id: Mapped[int] = mapped_column(BigInteger, ForeignKey('articles.id', ondelete="CASCADE"), nullable=False)
    filename: Mapped[str] = mapped_column(String(255), nullable=False)
    filesize: Mapped[int] = mapped_column(BigInteger, nullable=False)
    upload_date: Mapped[datetime] = mapped_column(TIMESTAMP(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now())

    article = relationship("Article", back_populates="assets")

    def __repr__(self):
        return f"<ArticleAsset(id={self.id}, filename='{self.filename}')>"

    def get_attrs(self):
        return {
            "asset_id": self.id,
            "article_id": self.article_id,
            "filename": self.filename,
            "filesize": self.filesize,
            "upload_date": self.upload_date
        }
