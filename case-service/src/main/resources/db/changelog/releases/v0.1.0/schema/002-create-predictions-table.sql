--liquibase formatted sql

--changeset eeki:schema-create-table-predictions
--comment Create predictions table for storing ML inference results
CREATE TABLE predictions (
     id           UUID                     NOT NULL,
     case_id      UUID                     NOT NULL,
     class_name   VARCHAR(64)              NOT NULL,
     confidence   DOUBLE PRECISION         NOT NULL,
     x_min        DOUBLE PRECISION         NOT NULL,
     y_min        DOUBLE PRECISION         NOT NULL,
     x_max        DOUBLE PRECISION         NOT NULL,
     y_max        DOUBLE PRECISION         NOT NULL,
     created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
     updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
     CONSTRAINT pk_predictions PRIMARY KEY (id),
     CONSTRAINT fk_predictions_case FOREIGN KEY (case_id) REFERENCES cases (id) ON DELETE CASCADE,
     CONSTRAINT chk_predictions_confidence CHECK (confidence >= 0 AND confidence <= 1),
     CONSTRAINT chk_predictions_x CHECK (x_min >= 0 AND x_max <= 1 AND x_min <= x_max),
     CONSTRAINT chk_predictions_y CHECK (y_min >= 0 AND y_max <= 1 AND y_min <= y_max)
);

CREATE INDEX idx_predictions_case_id ON predictions (case_id);

--rollback DROP TABLE predictions;