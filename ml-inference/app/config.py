"""
Конфигурация ml-inference, читается из переменных окружения.
"""
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Настройки приложения."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # Application
    app_name: str = "ml-inference"
    log_level: str = "INFO"

    # Kafka
    kafka_bootstrap_servers: str = "localhost:29092"
    kafka_consumer_group: str = "ml-inference-group"
    kafka_topic_image_uploaded: str = "cxr.image.uploaded"
    kafka_topic_inference_completed: str = "cxr.inference.completed"
    kafka_topic_inference_failed: str = "cxr.inference.failed"

    # S3-compatible storage (RustFS / MinIO)
    s3_endpoint: str = "http://localhost:9000"
    s3_access_key: str = "cxr_minio"
    s3_secret_key: str = "cxr_minio_pass"
    s3_bucket: str = "cxr-images"
    s3_region: str = "us-east-1"  # RustFS игнорирует, но boto3 требует

    # Model
    model_path: str = "/app/models/best.pt"
    model_version: str = "yolov8s-30ep"
    confidence_threshold: float = 0.05  # 0.25
    iou_threshold: float = 0.45


settings = Settings()