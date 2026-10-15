# Разработка

Руководство для локальной разработки: стек, структура, соглашения, частые задачи.

---

## Требования

- **Java 21** (для Java-сервисов, при разработке без Docker)
- **Node.js 20+** (для фронтенда)
- **Docker Desktop** (для запуска инфраструктуры)
- **Git**

---

## Быстрый старт

Полный запуск через Docker описан в [README.md](../README.md). Здесь — про разработку **в IDE**.

### Инфраструктура через Docker

Java-сервисы удобно запускать из IntelliJ IDEA, а инфраструктуру держать в Docker:

```bash
cd docker
docker compose up -d postgres minio kafka kafka-init kafka-ui
```

Тогда доступны:
- Postgres на `localhost:5433`
- RustFS на `localhost:9000` (API), `localhost:9001` (Console)
- Kafka на `localhost:29092` (с хоста)
- Kafka UI на `localhost:8090`

### ML-сервис

Для ML удобнее тоже Docker (там Python-зависимости):

```bash
docker compose up -d ml-inference
```

Или локально, если хочется отлаживать Python:

```bash
cd ml-inference
python -m venv .venv
source .venv/bin/activate  # или .venv\Scripts\activate на Windows
pip install -r requirements.txt
cp .env.example .env
# Отредактировать .env: KAFKA_BOOTSTRAP_SERVERS=localhost:29092, S3_ENDPOINT=http://localhost:9000
uvicorn app.main:app --reload
```

### Java-сервисы

1. Открыть каждый сервис в IDEA (`File → Open` → `user-service/pom.xml`).
2. Убедиться, что установлен JDK 21.
3. Запустить `XxxApplication` из IDE.

**Важно:** в `application.properties` должны быть указаны правильные адреса для локального запуска:
- Postgres: `jdbc:postgresql://localhost:5433/user_db` (и `case_db`)
- Kafka: `localhost:29092` (не `kafka:9092` — этот адрес только для docker-сети)
- RustFS: `http://localhost:9000`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Открывается на `http://localhost:3000`. Vite проксирует `/api/*` на `http://localhost:8080` (Gateway).

---

## Структура сервисов

### Стандартный layout Java-сервиса

```
xxx-service/
├── src/main/java/io/github/eeki/cxr/xxxservice/
│   ├── XxxApplication.java
│   ├── config/          # @Configuration классы
│   ├── controller/      # REST-контроллеры
│   ├── dto/             # Records (не классы) для запросов и ответов
│   ├── model/           # JPA-сущности и enum'ы
│   ├── repository/      # Spring Data репозитории
│   ├── security/        # JWT-компоненты
│   └── service/         # Бизнес-логика
├── src/main/resources/
│   ├── application.properties
│   ├── application-dev.properties
│   └── db/changelog/    # Liquibase-миграции
└── pom.xml
```

**Правила:**
- Контроллер не работает с сущностями напрямую — только через DTO.
- DTO — это **records**, не классы. Иммутабельны, меньше boilerplate.
- Сервисы возвращают DTO, а не JPA-сущности.
- `@Transactional` — только на методах сервиса, не на контроллере.

---

## Соглашения по коду

### Java

- **Пакеты:** `io.github.eeki.cxr.<service>.<layer>`.
- **DTO:** `record`, отдельные для request и response.
- **БД:** миграции через Liquibase, `ddl-auto=validate`.
- **Javadoc:** на классах и публичных методах. Внутри методов — комментарии только там, где логика неочевидна.
- **Логи:** через SLF4J. Использовать `@Slf4j` от Lombok, где он есть.
- **Исключения:** бизнес-ошибки — `IllegalArgumentException` / `IllegalStateException`; на границе API — `@ControllerAdvice` (планируется).

### Python

- **Пакеты:** `app/` в корне сервиса.
- **Схемы:** Pydantic-модели (`BaseModel`) в `schemas.py`.
- **Настройки:** pydantic-settings, читаются из env.
- **Async:** Kafka-клиент синхронный, крутится в отдельном потоке.

### TypeScript / React

- **Типы:** все API-типы в `src/types/api.ts`.
- **API-клиенты:** в `src/api/`, один файл на домен (auth, cases).
- **Страницы:** в `src/pages/`, компоненты — в `src/components/`.
- **Стейт:** серверный — React Query, клиентский — Context (для auth).
- **Стили:** Ant Design + inline-стили. CSS-файлы только для глобального.

### Коммиты

[Conventional Commits](https://www.conventionalcommits.org/) на русском:

```
<тип>(<scope>): <описание>

<тело: что изменилось и почему>
```

**Типы:** `feat`, `fix`, `chore`, `docs`, `refactor`, `test`.

**Примеры:**
```
feat(case-service): обработка результатов ML-инференса
fix(api-gateway): исправить порядок JWT-фильтра в цепочке
chore(docker): вынести секреты в .env
```

### Ветки

- `main` — стабильные релизы с тегами.
- `development` — основная рабочая ветка.
- `feature/<название>` — короткие ветки под фичи, вливаются в `development`.

---

## Частые задачи

### Добавить миграцию

1. Создать файл в `case-service/src/main/resources/db/changelog/releases/v0.1.0/schema/`.
2. Формат — `liquibase formatted sql`.
3. Обязательно `--rollback`.
4. Порядок операций: сначала снять констрейнты → изменить данные → навесить новые констрейнты.
5. Перезапустить сервис — Liquibase применит миграцию автоматически.

### Получить веса модели

Веса `best.pt` (~22 МБ) **не хранятся в git** — это требование действует и для запуска через Docker, и для локальной разработки. `ml-inference` читает их из папки `ml-inference/models/`.

**Где взять:** в проекте [rsna-pneumonia-yolo-pipeline](https://github.com/e-eki/rsna-pneumonia-yolo-pipeline) — файл `runs/train/2026-10-02_yolov8s_30ep/weights/best.pt`.

**Куда положить:** в `cxr-platform/ml-inference/models/best.pt`.

```bash
# Из корня репозитория cxr-platform
cp /path/to/rsna-pneumonia-yolo-pipeline/runs/train/2026-10-02_yolov8s_30ep/weights/best.pt \
   ml-inference/models/best.pt

# Проверить, что файл на месте и нужного размера (~22 МБ)
ls -lh ml-inference/models/best.pt
```

**Только после этого** запускайте `docker compose up -d --build` — иначе `ml-inference` упадёт при старте с ошибкой `Model weights not found`.

### Обновить ML-модель

1. Скопировать новый `best.pt` в `ml-inference/models/`.
2. Обновить `model_version` в `ml-inference/app/config.py`.
3. Перезапустить контейнер: `docker compose restart ml-inference`.

### Добавить новый Kafka-топик

1. Добавить имя в список топиков в `docker-compose.yml` (сервис `kafka-init`).
2. Пересоздать `kafka-init`: `docker compose rm -f kafka-init && docker compose up -d kafka-init`.
3. Проверить: `docker logs cxr-kafka-init`.

### Посмотреть сообщения в Kafka

**Через UI:** http://localhost:8090 → Topics → выбрать → Messages.

**Через CLI (Git Bash):**
```bash
MSYS_NO_PATHCONV=1 docker exec -it cxr-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic cxr.image.uploaded \
  --from-beginning
```

### Посмотреть файлы в RustFS

Открыть http://localhost:9001, залогиниться (`MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` из `.env`). Bucket `cxr-images` содержит загруженные снимки в подпапке `cases/`.

### Проверить health сервисов

```bash
curl http://localhost:8080/actuator/health  # api-gateway
curl http://localhost:8081/actuator/health  # case-service (если проброшен)
curl http://localhost:8082/actuator/health  # user-service (если проброшен)
curl http://localhost:8000/health           # ml-inference
```

В Docker-режиме порты 8081 и 8082 не проброшены наружу. Для локальных тестов — только через Gateway: `curl http://localhost:8080/api/cases -H "Authorization: Bearer <token>"`.

---

## Известные ограничения

- **Service-level JWT** в `case-service` и `user-service` отключён (`permitAll`). Gateway валидирует токены, но сами сервисы доверяют любому запросу из внутренней сети.
- **BaseEntity и другие общие классы** дублируются между сервисами. Планируется вынести в отдельный Maven-модуль `common`.

---

## Полезные ссылки

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Cloud Gateway](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
- [Liquibase SQL Format](https://docs.liquibase.com/concepts/changelogs/sql-format.html)
- [Ultralytics YOLOv8](https://docs.ultralytics.com/)
- [React Query](https://tanstack.com/query/latest)