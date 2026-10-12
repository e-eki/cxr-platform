"""
Pydantic-модели для событий Kafka.

Схемы должны совпадать с теми, что публикует case-service,
чтобы события корректно десериализовались.
"""
from datetime import datetime
from typing import List
from uuid import UUID

from pydantic import BaseModel, Field


class ImageUploadedEvent(BaseModel):
    """Событие: снимок загружен в хранилище и готов к анализу."""
    caseId: UUID
    imageKey: str
    uploadedAt: datetime


class Prediction(BaseModel):
    """Одно предсказание модели в нормализованных координатах [0, 1]."""
    className: str
    confidence: float
    xMin: float = Field(ge=0.0, le=1.0)
    yMin: float = Field(ge=0.0, le=1.0)
    xMax: float = Field(ge=0.0, le=1.0)
    yMax: float = Field(ge=0.0, le=1.0)


class InferenceCompletedEvent(BaseModel):
    """Событие: инференс завершён успешно."""
    caseId: UUID
    predictions: List[Prediction]
    durationMs: int
    modelVersion: str


class InferenceFailedEvent(BaseModel):
    """Событие: инференс не удался."""
    caseId: UUID
    error: str
    durationMs: int