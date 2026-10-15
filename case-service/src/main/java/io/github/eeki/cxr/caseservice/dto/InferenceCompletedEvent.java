package io.github.eeki.cxr.caseservice.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Событие: инференс завершён, ML-сервис вернул предсказания.
 *
 * <p>Публикуется сервисом ml-inference в топик cxr.inference.completed.</p>
 */
public record InferenceCompletedEvent(
        UUID caseId,
        List<PredictionDto> predictions,
        long durationMs,
        String modelVersion,
        Instant completedAt
) {
    /** Одно предсказание модели. */
    public record PredictionDto(
            String className,
            double confidence,
            double xMin,
            double yMin,
            double xMax,
            double yMax
    ) {}
}