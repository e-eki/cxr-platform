package io.github.eeki.cxr.caseservice.dto;

import io.github.eeki.cxr.caseservice.model.CaseStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Публичное представление кейса, возвращаемое клиенту.
 *
 * <p>Поле {@code predictions} пустое, пока ML не завершит анализ.
 * Заполняется автоматически при переходе кейса в статус
 * {@link CaseStatus#READY_FOR_REVIEW}.</p>
 */
public record CaseResponse(
        UUID id,
        String patientId,
        String imageKey,
        CaseStatus status,
        String comment,
        Instant createdAt,
        Instant updatedAt,
        int predictionsCount,
        List<PredictionResponse> predictions
) {
    public record PredictionResponse(
            String className,
            double confidence,
            double xMin,
            double yMin,
            double xMax,
            double yMax
    ) {}
}