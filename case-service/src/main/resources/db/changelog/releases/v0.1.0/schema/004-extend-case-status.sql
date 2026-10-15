--liquibase formatted sql

--changeset eeki:schema-extend-case-status
--comment Extend case status check: split REVIEWED into CONFIRMED and REJECTED

-- Шаг 1: удалить старую CHECK, чтобы можно было вставлять новые значения
ALTER TABLE cases DROP CONSTRAINT chk_cases_status;

-- Шаг 2: обновить данные — REVIEWED → CONFIRMED или REJECTED
-- на основании последнего решения из таблицы reviews
UPDATE cases
SET status = CASE
                 WHEN (SELECT decision FROM reviews
                       WHERE reviews.case_id = cases.id
                       ORDER BY reviews.created_at DESC LIMIT 1) = 'REJECTED'
        THEN 'REJECTED'
    ELSE 'CONFIRMED'
END
WHERE status = 'REVIEWED';

-- Шаг 3: добавить новую CHECK с расширенным набором статусов
ALTER TABLE cases ADD CONSTRAINT chk_cases_status
    CHECK (status IN ('UPLOADED', 'PROCESSING', 'READY_FOR_REVIEW',
                      'CONFIRMED', 'REJECTED', 'FAILED'));

--rollback ALTER TABLE cases DROP CONSTRAINT chk_cases_status;
--rollback UPDATE cases SET status = 'REVIEWED' WHERE status IN ('CONFIRMED', 'REJECTED');
--rollback ALTER TABLE cases ADD CONSTRAINT chk_cases_status
--rollback     CHECK (status IN ('UPLOADED', 'PROCESSING', 'READY_FOR_REVIEW', 'REVIEWED', 'FAILED'));