"""
FastAPI-приложение ml-inference.

При старте:
  1. Загружает YOLO-модель.
  2. Запускает Kafka-consumer в фоновом потоке.

При остановке:
  1. Останавливает Kafka-consumer.
  2. Дожидается отправки буферизованных сообщений.
"""
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, File, HTTPException, UploadFile

from app.config import settings
from app.kafka_consumer import KafkaConsumerWorker, create_consumer_worker
from app.model import ModelWrapper
from app.schemas import Prediction

# Логирование настроено один раз при импорте
logging.basicConfig(
    level=settings.log_level,
    format="%(asctime)s | %(levelname)s | %(name)s | %(message)s",
)
logger = logging.getLogger(__name__)

# Глобальные объекты — инициализируются в lifespan
_model: ModelWrapper | None = None
_consumer: KafkaConsumerWorker | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Управляет жизненным циклом ресурсов приложения."""
    global _model, _consumer

    logger.info("Starting %s...", settings.app_name)
    _model = ModelWrapper(settings.model_path)

    _consumer = create_consumer_worker(_model)
    _consumer.start()

    logger.info("%s is ready", settings.app_name)
    yield

    logger.info("Shutting down %s...", settings.app_name)
    if _consumer is not None:
        _consumer.stop()
    logger.info("%s stopped", settings.app_name)


app = FastAPI(
    title="CXR ML Inference Service",
    description="YOLO-based pneumonia detection for chest X-rays",
    version="0.1.0",
    lifespan=lifespan,
)


@app.get("/health")
def health() -> dict:
    """Проверка готовности сервиса."""
    if _model is None or _consumer is None:
        raise HTTPException(status_code=503, detail="Service is starting up")
    return {"status": "ok", "modelLoaded": True}


@app.get("/model/info")
def model_info() -> dict:
    """Информация о загруженной модели."""
    if _model is None:
        raise HTTPException(status_code=503, detail="Model not loaded")
    return {
        "path": settings.model_path,
        "version": settings.model_version,
        "confidenceThreshold": settings.confidence_threshold,
        "iouThreshold": settings.iou_threshold,
    }


@app.post("/predict", response_model=list[Prediction])
async def predict(file: UploadFile = File(...)) -> list[Prediction]:
    """
    Синхронный инференс — для тестов и демо.

    Принимает изображение, возвращает список предсказаний.
    """
    if _model is None:
        raise HTTPException(status_code=503, detail="Model not loaded")

    image_bytes = await file.read()
    if not image_bytes:
        raise HTTPException(status_code=400, detail="Empty file")

    try:
        return _model.predict(image_bytes)
    except Exception as e:
        logger.exception("Prediction failed")
        raise HTTPException(status_code=500, detail=str(e)) from e