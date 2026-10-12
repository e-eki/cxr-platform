--liquibase formatted sql

--changeset eeki:schema-create-table-reviews
--comment Create reviews table for storing doctor's decisions
CREATE TABLE reviews (
     id            UUID                     NOT NULL,
     case_id       UUID                     NOT NULL,
     reviewer_id   VARCHAR(64)              NOT NULL,
     decision      VARCHAR(32)              NOT NULL,
     comment       TEXT,
     created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
     updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
     CONSTRAINT pk_reviews PRIMARY KEY (id),
     CONSTRAINT fk_reviews_case FOREIGN KEY (case_id) REFERENCES cases (id) ON DELETE CASCADE,
     CONSTRAINT chk_reviews_decision CHECK (decision IN ('CONFIRMED', 'REJECTED'))
);

CREATE INDEX idx_reviews_case_id ON reviews (case_id);

--rollback DROP TABLE reviews;