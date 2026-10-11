package io.github.eeki.cxr.caseservice.dto;

import io.github.eeki.cxr.caseservice.model.CaseStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Публичное представление кейса, возвращаемое клиенту.
 */
public record CaseResponse(
        UUID id,
        String patientId,
        String imageKey,
        CaseStatus status,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {}
