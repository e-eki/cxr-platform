"""
Логика обработки одного события: скачать снимок, прогнать модель,
опубликовать результат.
"""
import logging
import time

from app.kafka_producer import kafka_producer
from app.model import ModelWrapper
from app.schemas import (
    ImageUploadedEvent,
    InferenceCompletedEvent,
    InferenceFailedEvent,
)
from app.storage import storage_client

logger = logging.getLogger(__name__)


def process_image_uploaded(event: ImageUploadedEvent, model: ModelWrapper) -> None:
    """
    Обрабатывает событие image.uploaded.

    Шаги:
      1. Скачивает снимок из объектного хранилища по imageKey.
      2. Прогоняет через YOLO.
      3. Публикует inference.completed (успех) или inference.failed (ошибка).
    """
    start = time.monotonic()
    logger.info("Processing case %s (imageKey=%s)", event.caseId, event.imageKey)

    try:
        image_bytes = storage_client.download_image(event.imageKey)
        predictions = model.predict(image_bytes)

        duration_ms = int((time.monotonic() - start) * 1000)
        completed = InferenceCompletedEvent(
            caseId=event.caseId,
            predictions=predictions,
            durationMs=duration_ms,
            modelVersion="yolov8s-30ep",
        )
        kafka_producer.publish_completed(completed)
        logger.info("Case %s: %d predictions in %d ms",
                    event.caseId, len(predictions), duration_ms)

    except Exception as e:
        duration_ms = int((time.monotonic() - start) * 1000)
        logger.exception("Case %s failed", event.caseId)
        kafka_producer.publish_failed(InferenceFailedEvent(
            caseId=event.caseId,
            error=str(e),
            durationMs=duration_ms,
        ))