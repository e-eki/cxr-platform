package io.github.eeki.cxr.caseservice.dto;

import io.github.eeki.cxr.caseservice.model.Review;

import java.time.Instant;
import java.util.UUID;

/**
 * Событие: кейс прошёл ревью.
 *
 * <p>Публикуется в Kafka в топик cxr.case.reviewed для аудита
 * и возможной дальнейшей обработки.</p>
 */
public record CaseReviewedEvent(
        UUID caseId,
        String reviewerId,
        Review.Decision decision,
        Instant reviewedAt
) {}