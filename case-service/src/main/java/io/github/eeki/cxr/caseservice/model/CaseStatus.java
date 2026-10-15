package io.github.eeki.cxr.caseservice.model;

/**
 * Статусы жизненного цикла кейса (снимка).
 *
 * <p>Переход: UPLOADED → PROCESSING → READY_FOR_REVIEW → CONFIRMED | REJECTED.
 * В случае ошибки обработки — FAILED.</p>
 */
public enum CaseStatus {

    /** Снимок загружен, ожидает отправки в ML-сервис. */
    UPLOADED,

    /** Снимок отправлен в ML-сервис, идёт анализ. */
    PROCESSING,

    /** Результат ML получен, ожидает ревью врача. */
    READY_FOR_REVIEW,

    /** Врач подтвердил результат ML. */
    CONFIRMED,

    /** Врач отклонил результат ML. */
    REJECTED,

    /** Обработка завершилась ошибкой. */
    FAILED
}