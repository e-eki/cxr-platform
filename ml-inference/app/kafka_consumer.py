"""
Фоновый Kafka-consumer, слушает топик image.uploaded и запускает инференс.
"""
import json
import logging
import threading

from confluent_kafka import Consumer, KafkaError, KafkaException

from app.config import settings
from app.inference import process_image_uploaded
from app.model import ModelWrapper
from app.schemas import ImageUploadedEvent

logger = logging.getLogger(__name__)


class KafkaConsumerWorker:
    """
    Запускает Kafka-consumer в отдельном потоке.

    confluent-kafka синхронный, поэтому нельзя крутить его в основном
    event loop FastAPI — нужен отдельный поток. Останавливается через
    флаг _running.
    """

    def __init__(self, model: ModelWrapper) -> None:
        self._model = model
        self._running = False
        self._thread: threading.Thread | None = None

        self._consumer = Consumer({
            "bootstrap.servers": settings.kafka_bootstrap_servers,
            "group.id": settings.kafka_consumer_group,
            "auto.offset.reset": "earliest",  # читаем с начала, если offset неизвестен
            "enable.auto.commit": False,       # коммитим вручную после успешной обработки
        })

    def start(self) -> None:
        """Запускает consumer в фоновом потоке."""
        self._running = True
        self._thread = threading.Thread(target=self._run, daemon=True)
        self._thread.start()
        logger.info("Kafka consumer started (group=%s, topic=%s)",
                    settings.kafka_consumer_group,
                    settings.kafka_topic_image_uploaded)

    def stop(self) -> None:
        """Останавливает consumer и ждёт завершения потока."""
        self._running = False
        if self._thread is not None:
            self._thread.join(timeout=10)
        self._consumer.close()
        logger.info("Kafka consumer stopped")

    def _run(self) -> None:
        """Основной цикл consumer'а."""
        self._consumer.subscribe([settings.kafka_topic_image_uploaded])

        while self._running:
            msg = self._consumer.poll(timeout=1.0)
            if msg is None:
                continue

            if msg.error():
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    continue
                logger.error("Kafka consumer error: %s", msg.error())
                continue

            self._handle_message(msg)

    def _handle_message(self, msg) -> None:
        """Разбирает сообщение и передаёт в обработку."""
        try:
            raw = msg.value().decode("utf-8")
            data = json.loads(raw)
            event = ImageUploadedEvent(**data)
        except Exception:
            logger.exception("Failed to parse Kafka message: %s", msg.value())
            # Смещаем offset, чтобы не застрять на битом сообщении
            self._consumer.commit(message=msg, asynchronous=False)
            return

        try:
            process_image_uploaded(event, self._model)
        except Exception:
            # Ошибка уже залогирована и отправлена как inference.failed,
            # значит сообщение обработано — коммитим offset
            logger.exception("Unhandled error while processing case %s", event.caseId)

        # Коммит после успешной обработки (at-least-once semantics)
        self._consumer.commit(message=msg, asynchronous=False)


def create_consumer_worker(model: ModelWrapper) -> KafkaConsumerWorker:
    """Фабрика — создаёт worker, но не запускает его."""
    return KafkaConsumerWorker(model)