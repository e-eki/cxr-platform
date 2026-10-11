--liquibase formatted sql

--changeset eeki:schema-create-table-cases
--comment Create cases table for storing X-ray metadata
CREATE TABLE cases (
    id           UUID                     NOT NULL,
    patient_id   VARCHAR(64)              NOT NULL,
    image_key    VARCHAR(255)             NOT NULL,
    status       VARCHAR(32)              NOT NULL,
    comment      TEXT,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_cases PRIMARY KEY (id),
    CONSTRAINT chk_cases_status CHECK (status IN ('UPLOADED', 'PROCESSING', 'READY_FOR_REVIEW', 'REVIEWED', 'FAILED'))
);

CREATE INDEX idx_cases_status ON cases (status);
CREATE INDEX idx_cases_patient_id ON cases (patient_id);
CREATE INDEX idx_cases_created_at ON cases (created_at DESC);

--rollback DROP TABLE cases;