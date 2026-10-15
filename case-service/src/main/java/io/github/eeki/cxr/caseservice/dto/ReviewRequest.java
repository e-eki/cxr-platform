package io.github.eeki.cxr.caseservice.dto;

import io.github.eeki.cxr.caseservice.model.Review;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Запрос на ревью кейса врачом.
 */
public record ReviewRequest(

        /** Решение врача: CONFIRMED или REJECTED. */
        @NotNull(message = "decision is required")
        Review.Decision decision,

        /** Комментарий (опционально, до 1000 символов). */
        @Size(max = 1000)
        String comment
) {}