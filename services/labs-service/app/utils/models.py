# Import downloaded modules
from uuid import UUID, uuid4
from sqlalchemy import ForeignKey, BigInteger, String, UUID, TIMESTAMP, Text, Boolean, Integer
from sqlalchemy.orm import Mapped, mapped_column, relationship, declarative_base
from sqlalchemy_serializer import SerializerMixin
from sqlalchemy.sql import func

# Import project files

Base = declarative_base()
class Lab(Base, SerializerMixin):
    __tablename__ = "labs"

    id: Mapped[UUID] = mapped_column(UUID, primary_key=True, default=uuid4)
    owner_id: Mapped[UUID] = mapped_column(UUID, nullable=False)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    created_at: Mapped[TIMESTAMP] = mapped_column(TIMESTAMP(timezone=True), nullable=False, server_default=func.now())
    updated_at: Mapped[TIMESTAMP] = mapped_column(TIMESTAMP(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now())
    abstract: Mapped[str] = mapped_column(Text, default="")
    views: Mapped[int] = mapped_column(BigInteger, default=0)
    submissions: Mapped[int] = mapped_column(BigInteger, default=0)
    stars: Mapped[int] = mapped_column(BigInteger, default=0)
    people_rated: Mapped[int] = mapped_column(BigInteger, default=0)

    lab_submissions = relationship("Submission", back_populates="lab")
    assets = relationship("LabAsset", back_populates="lab")
    articles = relationship("ArticleRelation", back_populates="lab")

    def __repr__(self):
        return f"<Lab(id={self.id}, title={self.title})>"

    def get_attrs(self):
        return {
            "id": self.id,
            "owner_id": self.owner_id,
            "title": self.title,
            "created_at": self.created_at,
            "updated_at": self.updated_at,
            "abstract": self.abstract,
            "views": self.views,
            "submissions": self.submissions,
            "stars": self.stars,
            "people_rated": self.people_rated
        }


class Submission(Base, SerializerMixin):
    __tablename__ = "submissions"

    id: Mapped[UUID] = mapped_column(UUID, primary_key=True, default=uuid4)
    lab_id: Mapped[UUID] = mapped_column(UUID, ForeignKey("labs.id", ondelete="CASCADE"), nullable=False)
    owner_id: Mapped[UUID] = mapped_column(UUID, nullable=False)
    created_at: Mapped[TIMESTAMP] = mapped_column(TIMESTAMP(timezone=True), nullable=False, server_default=func.now())
    updated_at: Mapped[TIMESTAMP] = mapped_column(TIMESTAMP(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now())
    status: Mapped[str] = mapped_column(String(50), nullable=False)
    points: Mapped[int] = mapped_column(Integer)

    lab = relationship("Lab", back_populates="lab_submissions")
    assets = relationship("SubmissionAsset", back_populates="submission")

    def __repr__(self):
        return f"<Submission(id={self.id}, lab_id={self.lab_id})>"

    def get_attrs(self):
        return {
            "id": str(self.id),
            "lab_id": str(self.lab_id),
            "owner_id": str(self.owner_id),
            "created_at": self.created_at,
            "updated_at": self.updated_at,
            "status": self.status,
            "points": self.points
        }


class ArticleRelation(Base, SerializerMixin):
    __tablename__ = "article_relations"

    lab_id: Mapped[UUID] = mapped_column(UUID, ForeignKey("labs.id", ondelete="CASCADE"), primary_key=True)
    article_id: Mapped[UUID] = mapped_column(UUID, primary_key=True)

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

    id: Mapped[UUID] = mapped_column(UUID, primary_key=True, default=uuid4)
    lab_id: Mapped[UUID] = mapped_column(UUID, ForeignKey("labs.id", ondelete="CASCADE"), nullable=False)
    filename: Mapped[str] = mapped_column(String(255), nullable=False)
    total_size: Mapped[int] = mapped_column(BigInteger, nullable=False)
    is_lab: Mapped[bool] = mapped_column(Boolean, default=False)
    upload_date: Mapped[TIMESTAMP] = mapped_column(TIMESTAMP(timezone=True), nullable=False, server_default=func.now())

    lab = relationship("Lab", back_populates="assets")

    def __repr__(self):
        return f"<LabAsset(id={self.id}, filename={self.filename})>"

    def get_attrs(self):
        return {
            "id": self.id,
            "lab_id": self.lab_id,
            "filename": self.filename,
            "total_size": self.total_size,
            "is_lab": self.is_lab,
            "upload_date": self.upload_date
        }


class SubmissionAsset(Base, SerializerMixin):
    __tablename__ = "submission_assets"

    id: Mapped[UUID] = mapped_column(UUID, primary_key=True, default=uuid4)
    solution_id: Mapped[UUID] = mapped_column(UUID, ForeignKey("submissions.id", ondelete="CASCADE"), nullable=False)
    filename: Mapped[str] = mapped_column(String(255), nullable=False)
    total_size: Mapped[int] = mapped_column(BigInteger, nullable=False)
    upload_date: Mapped[TIMESTAMP] = mapped_column(TIMESTAMP(timezone=True), nullable=False, server_default=func.now())

    submission = relationship("Submission", back_populates="assets")

    def __repr__(self):
        return f"<SubmissionAsset(id={self.id}, filename={self.filename})>"

    def get_attrs(self):
        return {
            "id": self.id,
            "solution_id": self.solution_id,
            "filename": self.filename,
            "total_size": self.total_size,
            "upload_date": self.upload_date
        }
