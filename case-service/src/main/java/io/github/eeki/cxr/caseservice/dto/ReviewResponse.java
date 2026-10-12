package io.github.eeki.cxr.caseservice.dto;

import io.github.eeki.cxr.caseservice.model.Review;

import java.time.Instant;
import java.util.UUID;

/**
 * Публичное представление ревью.
 */
public record ReviewResponse(
        UUID id,
        String reviewerId,
        Review.Decision decision,
        String comment,
        Instant createdAt
) {}