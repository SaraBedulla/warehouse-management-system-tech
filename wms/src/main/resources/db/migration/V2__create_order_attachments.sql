-- ─────────────────────────────────────────────────────────────────────────────
-- V3__create_order_attachments.sql
-- Stores file attachment metadata for orders.
-- Actual files live on disk at the path stored in file_path.
-- ─────────────────────────────────────────────────────────────────────────────

SET search_path TO wms;

CREATE TABLE wms3.order_attachments (
    id              BIGSERIAL       PRIMARY KEY,
    order_id        BIGINT          NOT NULL,
    uploaded_by_id  BIGINT          NOT NULL,
    original_name   VARCHAR(255)    NOT NULL,
    stored_name     VARCHAR(255)    NOT NULL,
    file_path       VARCHAR(512)    NOT NULL,
    content_type    VARCHAR(100)    NOT NULL,
    file_size       BIGINT          NOT NULL,
    uploaded_at     TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_attachments_order     FOREIGN KEY (order_id)
                                            REFERENCES wms3.orders (id)
                                            ON DELETE CASCADE,
    CONSTRAINT fk_attachments_uploader  FOREIGN KEY (uploaded_by_id)
                                            REFERENCES wms3.users (id)
                                            ON DELETE RESTRICT
);

CREATE INDEX idx_attachments_order_id ON wms3.order_attachments (order_id);
