-- ─────────────────────────────────────────────────────────────────────────────
-- V0__create_schema.sql
-- Creates the application schema before any tables are built.
-- All subsequent migrations and the application itself operate inside wms.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE SCHEMA IF NOT EXISTS wms;

-- Set the search path so all subsequent DDL and DML in this session
-- resolve to the wms schema without needing to prefix every object.
SET search_path TO wms;
