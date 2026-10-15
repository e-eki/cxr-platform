# Архитектура

Обзор технической архитектуры CXR Platform: компоненты, поток данных, ключевые решения.

---

## Общая схема

```text
                           ┌──────────────────┐
                           │   Frontend       │
                           │   React + Vite   │
                           │   :3000          │
                           └────────┬─────────┘
                                    │ HTTP/JSON
                                    ▼
                           ┌──────────────────┐
                           │   API Gateway    │
                           │  Spring Cloud    │
                           │   :8080          │
                           │  JWT validation  │
                           └────────┬─────────┘
                                    │
              ┌─────────────────────┼─────────────────────┐
              │                     │                     │
              ▼                     ▼                     ▼
      ┌───────────────┐    ┌───────────────┐    ┌───────────────┐
      │ user-service  │    │ case-service  │    │ ml-inference  │
      │ Spring Boot   │    │ Spring Boot   │    │ FastAPI+ YOLO │
      │ :8082         │    │ :8081         │    │ :8000         │
      └───────┬───────┘    └───────┬───────┘    └───────┬───────┘
              │                    │                    │
              │                    │                    │
              ▼                    ▼                    ▼
      ┌───────────────┐    ┌───────────────┐    ┌───────────────┐
      │   user_db     │    │   case_db     │    │  RustFS (S3)  │
      │  PostgreSQL   │    │  PostgreSQL   │    │   :9000       │
      └───────────────┘    └───────────────┘    └───────────────┘
                                    │
                                    │ Kafka events
                                    ▼
                           ┌──────────────────┐
                           │      Kafka       │
                           │  KRaft, :9092    │
                           └──────────────────┘
```

---

## Компоненты

### API Gateway

- **Стек:** Spring Cloud Gateway (reactive, WebFlux)
- **Порт:** 8080

Единая точка входа для фронтенда. Задачи:

- Маршрутизация запросов к сервисам (`/api/auth/**` → user-service, `/api/cases/**` → case-service, `/api/predict/**` → ml-inference).
- Валидация JWT. Успешно провалидированный запрос обогащается заголовками `X-User-Id` и `X-User-Role`.
- CORS для фронтенда.
- Публичные пути: `/api/auth/**`, `/actuator/**`, `/api/cases/*/image` (последнее — потому что `<img>` в браузере не передаёт заголовки).

### user-service

- **Стек:** Spring Boot 3.4, Spring Security
- **БД:** `user_db`
- **Порт:** 8082

Отвечает за аутентификацию:

- Регистрация нового пользователя (роль по умолчанию — `DOCTOR`).
- Логин: проверка BCrypt-хэша, выдача JWT с claim `role`.
- BCrypt-хэширование паролей.
- В dev-профиле: `DevAdminSeeder` создаёт админа `admin/admin123` при первом запуске.

### case-service

- **Стек:** Spring Boot 3.4, Spring Data JPA, Spring Kafka
- **БД:** `case_db`
- **Порт:** 8081

Основной рабочий сервис:

- Создание кейса: загрузка файла в RustFS, сохранение метаданных в БД, публикация события `image.uploaded`.
- Kafka-consumer: обработка `inference.completed` и `inference.failed` от ML-сервиса.
- Сохранение bbox-ов в таблицу `predictions` (идемпотентно — старые удаляются перед вставкой).
- Смена статуса кейса: `UPLOADED` → `READY_FOR_REVIEW` → `CONFIRMED` / `REJECTED`.
- Ревью: сохранение решения врача, публикация события `case.reviewed`.
- Стрим снимков из RustFS.

### ml-inference

- **Стек:** Python 3.11, FastAPI, Ultralytics YOLOv8
- **Порт:** 8000

ML-сервис:

- Kafka-consumer слушает `image.uploaded`.
- Скачивает снимок из RustFS по ключу.
- Получает предсказания YOLO-модели - bbox-ы очагов в нормализованных координатах `[0, 1]`.
- Публикует `inference.completed` с результатами.
- При ошибке — публикует `inference.failed`.
- Дополнительно: синхронный эндпоинт `/predict` для тестов.

### frontend

- **Стек:** React 18, TypeScript, Vite, Ant Design, React Query
- **Порт:** 3000 (в dev), 80 (в Docker через Nginx)

- Логин и регистрация.
- Список кейсов с автообновлением (polling каждые 5 секунд).
- Детали кейса: снимок с SVG-оверлеем bbox-ов.
- Форма ревью: подтвердить / отклонить + комментарий.

React Query кэширует запросы и обновляет данные в фоне, пока кейс находится в статусе `UPLOADED` или `PROCESSING`.

### Инфраструктура

- **PostgreSQL 16** — две БД по (паттерну database-per-service).
- **Apache Kafka (KRaft)** — брокер событий, топики создаются контейнером `kafka-init`.
- **RustFS** — S3-совместимое объектное хранилище для снимков.

---

## Поток данных

### Загрузка снимка и ML-анализ

```text
1. Frontend POST /api/cases (multipart)
2. Gateway: валидация JWT → добавляет X-User-Id
3. case-service: 
   - генерирует imageKey = cases/<uuid>-<filename>
   - сохраняет файл в RustFS
   - создаёт запись в БД (status = UPLOADED)
   - публикует image.uploaded {caseId, imageKey, uploadedAt}
4. ml-inference:
   - читает событие из Kafka
   - скачивает файл из RustFS по imageKey
   - получает предсказания YOLO
   - публикует inference.completed {caseId, predictions[], durationMs}
5. case-service (consumer):
   - удаляет старые predictions для caseId (идемпотентность)
   - сохраняет новые predictions
   - меняет статус на READY_FOR_REVIEW
6. Frontend: через polling получает обновлённый кейс с bbox-ами
```

### Ревью

```text
1. Врач: POST /api/cases/{id}/review {decision, comment}
2. Gateway: валидация JWT → X-User-Id
3. case-service:
   - проверяет, что статус = READY_FOR_REVIEW
   - сохраняет запись в reviews
   - меняет статус на CONFIRMED или REJECTED
   - публикует case.reviewed {caseId, reviewerId, decision}
```

---

## Kafka-топики

| Топик | Продюсер | Consumer | Payload |
|---|---|---|---|
| `cxr.image.uploaded` | case-service | ml-inference | `{caseId, imageKey, uploadedAt}` |
| `cxr.inference.completed` | ml-inference | case-service | `{caseId, predictions[], durationMs, modelVersion}` |
| `cxr.inference.failed` | ml-inference | case-service | `{caseId, error, durationMs}` |
| `cxr.case.reviewed` | case-service | — (для аудита) | `{caseId, reviewerId, decision, reviewedAt}` |

**Гарантии доставки:** at-least-once. Обработка идемпотентна: `applyInferenceResult` удаляет старые predictions перед вставкой новых.

---

## Схема БД

### `user_db`

**`users`:** `id` (UUID, PK), `username` (unique), `email` (unique), `password` (BCrypt), `role` (`DOCTOR`/`ADMIN`), `created_at`, `updated_at`.

### `case_db`

**`cases`:** `id` (UUID, PK), `patient_id`, `image_key`, `status`, `comment`, `created_at`, `updated_at`.
Индексы: `status`, `patient_id`, `created_at`.

**`predictions`:** `id` (UUID, PK), `case_id` (FK, ON DELETE CASCADE), `class_name`, `confidence`, `x_min`, `y_min`, `x_max`, `y_max` (нормализованные `[0, 1]`), `created_at`, `updated_at`.

**`reviews`:** `id` (UUID, PK), `case_id` (FK, ON DELETE CASCADE), `reviewer_id` (string), `decision` (`CONFIRMED`/`REJECTED`), `comment`, `created_at`, `updated_at`.

**Миграции:** Liquibase, файлы в `user-service/src/main/resources/db/changelog/` и `case-service/src/main/resources/db/changelog/`.

---

## Ключевые технические решения

### Database-per-service

У каждого сервиса своя БД (`user_db`, `case_db`). Сервисы не читают таблицы друг друга. Идентификатор врача в `reviews.reviewer_id` хранится как строка — это значение из чужого сервиса, FK между базами не делается.

### Идемпотентность при обработке событий

`case-service.applyInferenceResult` перед сохранением новых predictions удаляет старые для того же кейса. Если Kafka повторно доставит то же событие — дублей не будет.

### Публичный доступ к `/api/cases/{id}/image`

Стандартный тег `<img>` в браузере не передаёт заголовок `Authorization`, поэтому Gateway разрешает этот путь без JWT. UUID кейса — непредсказуемый идентификатор, его невозможно перебрать.

### Парные статусы ревью

Вместо одного статуса `REVIEWED` используются `CONFIRMED` и `REJECTED`. Это упрощает фильтрацию и статистику, и отражает семантику: подтверждение и отклонение — два разных исхода.

### Defense in depth

В production-версии JWT валидируется дважды: на Gateway (первый барьер) и в каждом сервисе (второй барьер). Сейчас в сервисах стоит `permitAll` для упрощения разработки — service-level JWT планируется как следующий шаг.

---

## Ограничения текущей версии

- Модель yolov8s с mAP@0.5 = 0.349 — работает как демонстрация, не готова к применению в проде.
- Service-level JWT в user-service и case-service отключён.
- Нет миграций для отката изменений схемы в production-режиме.
- Kafka в single-node режиме, без репликации.
- Нет автoscaling и мониторинга (Prometheus/Grafana).

Это упрощения для портфолио. В реальном проекте каждый пункт решается отдельно.