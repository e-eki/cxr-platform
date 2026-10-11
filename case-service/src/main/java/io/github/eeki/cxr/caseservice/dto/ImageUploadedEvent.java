package io.github.eeki.cxr.caseservice.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Событие: снимок загружен в хранилище и готов к анализу.
 *
 * <p>Публикуется в Kafka в топик {@code cxr.image.uploaded} при создании
 * кейса. Потребитель — сервис {@code ml-inference}, который скачает снимок
 * из объектного хранилища по {@code imageKey} и прогонит через модель.</p>
 */
public record ImageUploadedEvent(
        UUID caseId,
        String imageKey,
        Instant uploadedAt
) {}
