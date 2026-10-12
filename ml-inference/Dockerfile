# ─── Build stage ──────────────────────────────────────────
FROM python:3.11-slim AS builder

WORKDIR /build

# Системные зависимости для сборки (opencv, confluent-kafka)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    librdkafka-dev \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir --user -r requirements.txt

# ─── Runtime stage ────────────────────────────────────────
FROM python:3.11-slim

WORKDIR /app

# Runtime-зависимости для opencv (нужны libGL и libglib)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    librdkafka1 \
    && rm -rf /var/lib/apt/lists/*

# Копируем установленные пакеты из builder-стадии
COPY --from=builder /root/.local /root/.local
ENV PATH=/root/.local/bin:$PATH

# Копируем код и папку для весов
COPY app/ ./app/
RUN mkdir -p /app/models

# Открываем порт FastAPI
EXPOSE 8000

# Запускаем uvicorn
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]