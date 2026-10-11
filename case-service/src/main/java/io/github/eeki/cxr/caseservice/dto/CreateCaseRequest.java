package io.github.eeki.cxr.caseservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Запрос на создание кейса.
 *
 * <p>Пока передаётся только анонимизированный patientId и ключ объекта
 * в объектном хранилище. Загрузка файла будет добавлена позже через MinIO.</p>
 */
public record CreateCaseRequest(

        /** Анонимизированный идентификатор пациента. */
        @NotBlank(message = "patientId is required")
        @Size(max = 64)
        String patientId,

        /** Ключ объекта в объектном хранилище. */
        @NotBlank(message = "imageKey is required")
        @Size(max = 255)
        String imageKey
) {}
