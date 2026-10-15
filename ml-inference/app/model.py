# Импорт io нужен для BytesIO
import io  # noqa: E402

"""
Обёртка вокруг YOLO-модели из проекта rsna-pneumonia-yolo-pipeline.
"""
import logging
from pathlib import Path
from typing import List

import numpy as np
from PIL import Image
from ultralytics import YOLO

from app.config import settings
from app.schemas import Prediction

logger = logging.getLogger(__name__)


class ModelWrapper:
    """
    Загружает YOLO-модель один раз и умеет прогонять изображения.

    Загружается при старте приложения (singleton), потому что загрузка
    весов занимает несколько секунд и не должна происходить на каждый запрос.
    """

    def __init__(self, model_path: str) -> None:
        path = Path(model_path)
        if not path.exists():
            raise FileNotFoundError(f"Model weights not found: {model_path}")

        logger.info("Loading YOLO model from %s", model_path)
        self._model = YOLO(str(path))
        self._class_names = self._model.names  # {0: "pneumonia", ...}
        logger.info("Model loaded. Classes: %s", self._class_names)

    def predict(self, image_bytes: bytes) -> List[Prediction]:
        """
        Прогоняет изображение через модель.

        Возвращает список предсказаний в нормализованных координатах [0, 1].

        :param image_bytes: содержимое изображения (PNG/JPEG)
        """
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        width, height = image.size

        results = self._model.predict(
            source=np.array(image),
            conf=settings.confidence_threshold,
            iou=settings.iou_threshold,
            verbose=False,
        )

        predictions: List[Prediction] = []
        for box in results[0].boxes:
            # xyxy — координаты в пикселях
            x1, y1, x2, y2 = box.xyxy[0].tolist()
            confidence = float(box.conf[0])
            class_id = int(box.cls[0])

            predictions.append(Prediction(
                className=self._class_names[class_id],
                confidence=round(confidence, 4),
                xMin=round(max(0.0, x1 / width), 6),
                yMin=round(max(0.0, y1 / height), 6),
                xMax=round(min(1.0, x2 / width), 6),
                yMax=round(min(1.0, y2 / height), 6),
            ))

        logger.info("Model returned %d predictions", len(predictions))
        return predictions
