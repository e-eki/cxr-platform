"""
Работа с S3-совместимым объектным хранилищем (RustFS / MinIO).
"""
import io
import logging

import boto3
from botocore.client import Config
from botocore.exceptions import ClientError

from app.config import settings

logger = logging.getLogger(__name__)


class StorageClient:
    """Обёртка над boto3 для скачивания снимков из объектного хранилища."""

    def __init__(self) -> None:
        self._client = boto3.client(
            "s3",
            endpoint_url=settings.s3_endpoint,
            aws_access_key_id=settings.s3_access_key,
            aws_secret_access_key=settings.s3_secret_key,
            region_name=settings.s3_region,
            config=Config(signature_version="s3v4"),
        )
        self._bucket = settings.s3_bucket

    def download_image(self, image_key: str) -> bytes:
        """
        Скачивает файл из хранилища и возвращает его содержимое в виде байтов.

        :raises RuntimeError: если файл не найден или хранилище недоступно
        """
        try:
            response = self._client.get_object(Bucket=self._bucket, Key=image_key)
            data = response["Body"].read()
            logger.info("Downloaded %s (%d bytes) from bucket %s",
                        image_key, len(data), self._bucket)
            return data
        except ClientError as e:
            error_code = e.response.get("Error", {}).get("Code", "Unknown")
            logger.error("Failed to download %s: %s", image_key, e)
            raise RuntimeError(f"S3 download failed ({error_code}): {image_key}") from e


# Singleton — создаётся один раз при импорте
storage_client = StorageClient()