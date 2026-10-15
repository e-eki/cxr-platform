-- Runs once when the Postgres container initializes an empty data directory.
-- Creates one database per microservice (database-per-service pattern).
-- Executed as POSTGRES_USER (cxr_user).

CREATE DATABASE user_db OWNER cxr_user;
CREATE DATABASE case_db OWNER cxr_user;
