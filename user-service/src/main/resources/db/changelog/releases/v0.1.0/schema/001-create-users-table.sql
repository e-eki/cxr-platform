--liquibase formatted sql

--changeset eeki:schema-create-table-users
--comment Create users table for authentication
CREATE TABLE users (
    id          UUID                     NOT NULL,
    username    VARCHAR(64)              NOT NULL,
    email       VARCHAR(128)             NOT NULL,
    password    VARCHAR(255)             NOT NULL,
    role        VARCHAR(32)              NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('DOCTOR', 'ADMIN'))
);
--rollback DROP TABLE users;