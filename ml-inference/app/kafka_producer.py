"""
Публикация событий о результатах инференса в Kafka.
"""
import json
import logging
import threading

from confluent_kafka import Producer

from app.config import settings
from app.schemas import InferenceCompletedEvent, InferenceFailedEvent

logger = logging.getLogger(__name__)


class KafkaEventProducer:
    """Потокобезопасный продюсер событий в Kafka с ленивой инициализацией."""

    def __init__(self) -> None:
        self._producer: Producer | None = None
        self._lock = threading.Lock()

    def _get_producer(self) -> Producer:
        """Создаёт Producer при первом обращении."""
        if self._producer is None:
            with self._lock:
                if self._producer is None:
                    self._producer = Producer({
                        "bootstrap.servers": settings.kafka_bootstrap_servers,
                        "client.id": f"{settings.app_name}-producer",
                        "enable.idempotence": True,
                        "acks": "all",
                    })
                    logger.info("Kafka producer created (bootstrap=%s)",
                                settings.kafka_bootstrap_servers)
        return self._producer

    def _delivery_report(self, err, msg) -> None:
        """Callback: логирует результат доставки сообщения."""
        if err is not None:
            logger.error("Kafka delivery failed: %s", err)
        else:
            logger.info("Kafka message delivered to %s [partition %d]",
                        msg.topic(), msg.partition())

    def _publish(self, topic: str, key: str, payload: dict) -> None:
        """Внутренний метод: сериализация и отправка."""
        producer = self._get_producer()
        producer.produce(
            topic=topic,
            key=key.encode("utf-8"),
            value=json.dumps(payload, default=str).encode("utf-8"),
            callback=self._delivery_report,
        )
        producer.poll(0)

    def publish_completed(self, event: InferenceCompletedEvent) -> None:
        """Публикует событие об успешном инференсе."""
        self._publish(
            topic=settings.kafka_topic_inference_completed,
            key=str(event.caseId),
            payload=event.model_dump(mode="json"),
        )

    def publish_failed(self, event: InferenceFailedEvent) -> None:
        """Публикует событие об ошибке инференса."""
        self._publish(
            topic=settings.kafka_topic_inference_failed,
            key=str(event.caseId),
            payload=event.model_dump(mode="json"),
        )

    def flush(self, timeout: float = 5.0) -> None:
        """Ожидает отправки всех буферизованных сообщений."""
        if self._producer is not None:
            self._producer.flush(timeout)


# Singleton (ленивый)
kafka_producer = KafkaEventProducer()