# Import downloaded modules
from datetime import datetime
from typing import List, Optional

# Import built-in modules
from sqlalchemy import BigInteger, ForeignKey, String, Text, Integer, DateTime, TIMESTAMP
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.sql import func
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy_serializer import SerializerMixin

Base = declarative_base()

class Lab(Base, SerializerMixin):
    __tablename__ = "labs"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    owner_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    created_at: Mapped[datetime] = mapped_column(TIMESTAMP(timezone=True), nullable=False, server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(TIMESTAMP(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now())
    abstract: Mapped[Optional[str]] = mapped_column(Text)
    views: Mapped[int] = mapped_column(BigInteger, default=0)
    submissions: Mapped[int] = mapped_column(BigInteger, default=0)
    stars: Mapped[int] = mapped_column(BigInteger, default=0)
    people_rated: Mapped[int] = mapped_column(BigInteger, default=0)

    # Relationships
    lab_submissions = relationship("Submission", back_populates="lab", cascade="all, delete")
    assets = relationship("LabAsset", back_populates="lab", cascade="all, delete")
    articles = relationship("ArticleRelation", back_populates="lab", cascade="all, delete")

    def __repr__(self):
        return f"<Lab(id={self.id}, title={self.title})>"

    def get_attrs(self):
        return {
            "lab_id": self.id,
            "owner_id": self.owner_id,
            "title": self.title,
            "created_at": self.created_at,
            "updated_at": self.updated_at,
            "abstract": self.abstract,
            "views": self.views,
            "submissions": self.submissions,
            "stars_total": self.stars,
            "people_rated": self.people_rated
        }


class ArticleRelation(Base, SerializerMixin):
    __tablename__ = "article_relations"

    lab_id: Mapped[int] = mapped_column(BigInteger, ForeignKey("labs.id", ondelete="CASCADE"), primary_key=True)
    article_id: Mapped[int] = mapped_column(BigInteger, primary_key=True)

    # Relationships
    lab = relationship("Lab", back_populates="articles")

    def __repr__(self):
        return f"<ArticleRelation(lab_id={self.lab_id}, article_id={self.article_id})>"

    def get_attrs(self):
        return {
            "lab_id": self.lab_id,
            "article_id": self.article_id
        }


class LabAsset(Base, SerializerMixin):
    __tablename__ = "lab_assets"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    lab_id: Mapped[int] = mapped_column(BigInteger, ForeignKey("labs.id", ondelete="CASCADE"), nullable=False)
    filename: Mapped[str] = mapped_column(String(255), nullable=False)
    filesize: Mapped[int] = mapped_column(BigInteger, nullable=False)
    upload_date: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, server_default=func.now())

    # Relationships
    lab = relationship("Lab", back_populates="assets")

    def __repr__(self):
        return f"<LabAsset(id={self.id}, lab_id={self.lab_id}, filename={self.filename})>"

    def get_attrs(self):
        return {
            "asset_id": self.id,
            "lab_id": self.lab_id,
            "filename": self.filename,
            "filesize": self.filesize,
            "upload_date": self.upload_date
        }

