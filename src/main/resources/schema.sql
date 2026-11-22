-- Lat cat cua thiet ke D02 muc 2: cac bang can cho F01 (dang nhap, phan quyen)
-- va F07 (ban hang tai quay). Ten bang va rang buoc giu dung nhu D02.

DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS stock_movement;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS sales_invoice_line;
DROP TABLE IF EXISTS sales_invoice;
DROP TABLE IF EXISTS stock_balance;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS user_assignment;
DROP TABLE IF EXISTS app_user;
DROP TABLE IF EXISTS warehouse;

CREATE TABLE warehouse (
    id      BIGSERIAL PRIMARY KEY,
    code    VARCHAR(20)  NOT NULL UNIQUE,
    name    VARCHAR(120) NOT NULL,
    type    VARCHAR(10)  NOT NULL CHECK (type IN ('CENTRAL', 'BRANCH')),
    active  BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE app_user (
    id                  BIGSERIAL PRIMARY KEY,
    username            VARCHAR(50)  NOT NULL UNIQUE,
    password_hash       VARCHAR(100) NOT NULL,
    full_name           VARCHAR(120) NOT NULL,
    failed_login_count  INT          NOT NULL DEFAULT 0,
    locked_until        TIMESTAMP,
    active              BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Vai tro gan theo chi nhanh. warehouse_id NULL = toan chuoi.
CREATE TABLE user_assignment (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT      NOT NULL REFERENCES app_user (id),
    role          VARCHAR(30) NOT NULL,
    warehouse_id  BIGINT      REFERENCES warehouse (id)
);

CREATE TABLE product (
    id         BIGSERIAL PRIMARY KEY,
    sku        VARCHAR(40)    NOT NULL UNIQUE,
    barcode    VARCHAR(40)    NOT NULL UNIQUE,
    name       VARCHAR(200)   NOT NULL,
    price      NUMERIC(15, 0) NOT NULL CHECK (price >= 0),
    min_stock  INT            NOT NULL DEFAULT 0,
    status     VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE'
);

-- Ton kho theo cap (kho, san pham). Rang buoc CHECK la lop chan ton am thu hai (D02 muc 2.2).
CREATE TABLE stock_balance (
    warehouse_id  BIGINT NOT NULL REFERENCES warehouse (id),
    product_id    BIGINT NOT NULL REFERENCES product (id),
    qty           INT    NOT NULL CHECK (qty >= 0),
    version       BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (warehouse_id, product_id)
);

CREATE TABLE sales_invoice (
    id               BIGSERIAL PRIMARY KEY,
    warehouse_id     BIGINT         NOT NULL REFERENCES warehouse (id),
    cashier_id       BIGINT         NOT NULL REFERENCES app_user (id),
    total            NUMERIC(15, 0) NOT NULL CHECK (total >= 0),
    status           VARCHAR(20)    NOT NULL,
    einvoice_status  VARCHAR(20)    NOT NULL,
    created_at       TIMESTAMP      NOT NULL
);

CREATE TABLE sales_invoice_line (
    id          BIGSERIAL PRIMARY KEY,
    invoice_id  BIGINT         NOT NULL REFERENCES sales_invoice (id),
    product_id  BIGINT         NOT NULL REFERENCES product (id),
    qty         INT            NOT NULL CHECK (qty > 0),
    unit_price  NUMERIC(15, 0) NOT NULL CHECK (unit_price >= 0)
);

CREATE TABLE payment (
    id          BIGSERIAL PRIMARY KEY,
    invoice_id  BIGINT         NOT NULL REFERENCES sales_invoice (id),
    method      VARCHAR(20)    NOT NULL CHECK (method IN ('CASH', 'TRANSFER', 'CARD')),
    amount      NUMERIC(15, 0) NOT NULL CHECK (amount > 0)
);

-- So bien dong kho: chi INSERT, dung de doi soat (D02 muc 2).
CREATE TABLE stock_movement (
    id            BIGSERIAL PRIMARY KEY,
    warehouse_id  BIGINT      NOT NULL REFERENCES warehouse (id),
    product_id    BIGINT      NOT NULL REFERENCES product (id),
    qty_change    INT         NOT NULL,
    ref_type      VARCHAR(30) NOT NULL,
    ref_id        BIGINT      NOT NULL,
    created_by    BIGINT      NOT NULL REFERENCES app_user (id),
    created_at    TIMESTAMP   NOT NULL
);

CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT,
    action      VARCHAR(50)  NOT NULL,
    entity      VARCHAR(50),
    entity_id   BIGINT,
    detail      VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL
);
