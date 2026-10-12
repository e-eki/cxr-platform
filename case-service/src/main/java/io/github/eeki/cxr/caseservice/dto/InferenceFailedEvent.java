package io.github.eeki.cxr.caseservice.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Событие: инференс завершился ошибкой.
 *
 * <p>Публикуется сервисом ml-inference в топик cxr.inference.failed.
 * Case-service помечает кейс как FAILED и сохраняет текст ошибки.</p>
 */
public record InferenceFailedEvent(
        UUID caseId,
        String error,
        long durationMs,
        Instant failedAt
) {}