-- ─────────────────────────────────────────────────────────────────────────────
-- V1__create_tables.sql
-- Creates all tables inside the wms schema.
-- ─────────────────────────────────────────────────────────────────────────────

SET search_path TO wms3;

-- ─── ENUM TYPES ──────────────────────────────────────────────────────────────

CREATE TYPE wms3.role_type AS ENUM (
    'CLIENT',
    'WAREHOUSE_MANAGER',
    'SYSTEM_ADMIN'
);

CREATE TYPE wms3.order_status AS ENUM (
    'CREATED',
    'AWAITING_APPROVAL',
    'APPROVED',
    'DECLINED',
    'FULFILLED',
    'CANCELED'
);

-- ─── USERS ───────────────────────────────────────────────────────────────────

CREATE TABLE wms3.users (
    id          BIGSERIAL           PRIMARY KEY,
    username    VARCHAR(80)         NOT NULL,
    password    VARCHAR(255)        NOT NULL,
    full_name   VARCHAR(100)        NOT NULL,
    email       VARCHAR(120)        NOT NULL,
    role        wms3.role_type       NOT NULL,
    enabled     BOOLEAN             NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_users_username    UNIQUE (username),
    CONSTRAINT uq_users_email       UNIQUE (email)
);

-- ─── INVENTORY ITEMS ─────────────────────────────────────────────────────────

CREATE TABLE wms3.inventory_items (
    id          BIGSERIAL           PRIMARY KEY,
    item_name   VARCHAR(150)        NOT NULL,
    quantity    INTEGER             NOT NULL,
    unit_price  NUMERIC(10, 2)      NOT NULL,

    CONSTRAINT chk_inventory_quantity   CHECK (quantity >= 0),
    CONSTRAINT chk_inventory_unit_price CHECK (unit_price > 0)
);

-- ─── ORDERS ──────────────────────────────────────────────────────────────────

CREATE TABLE wms3.orders (
    id              BIGSERIAL           PRIMARY KEY,
    order_number    VARCHAR(20)         NOT NULL,
    client_id       BIGINT              NOT NULL,
    status          wms3.order_status    NOT NULL DEFAULT 'CREATED',
    submitted_date  TIMESTAMP           NOT NULL DEFAULT NOW(),
    deadline_date   DATE,
    decline_reason  VARCHAR(500),

    CONSTRAINT uq_orders_order_number   UNIQUE (order_number),
    CONSTRAINT fk_orders_client         FOREIGN KEY (client_id)
                                            REFERENCES wms3.users (id)
                                            ON DELETE RESTRICT
);

-- ─── ORDER ITEMS ─────────────────────────────────────────────────────────────

CREATE TABLE wms3.order_items (
    id                  BIGSERIAL   PRIMARY KEY,
    order_id            BIGINT      NOT NULL,
    inventory_item_id   BIGINT      NOT NULL,
    quantity            INTEGER     NOT NULL,

    CONSTRAINT chk_order_items_quantity     CHECK (quantity > 0),
    CONSTRAINT fk_order_items_order         FOREIGN KEY (order_id)
                                                REFERENCES wms3.orders (id)
                                                ON DELETE CASCADE,
    CONSTRAINT fk_order_items_inventory     FOREIGN KEY (inventory_item_id)
                                                REFERENCES wms3.inventory_items (id)
                                                ON DELETE RESTRICT
);

-- ─── INDEXES ─────────────────────────────────────────────────────────────────

CREATE INDEX idx_users_username                 ON wms3.users (username);
CREATE INDEX idx_users_email                    ON wms3.users (email);
CREATE INDEX idx_orders_client_id               ON wms3.orders (client_id);
CREATE INDEX idx_orders_status                  ON wms3.orders (status);
CREATE INDEX idx_order_items_order_id           ON wms3.order_items (order_id);
CREATE INDEX idx_order_items_inventory_item_id  ON wms3.order_items (inventory_item_id);
