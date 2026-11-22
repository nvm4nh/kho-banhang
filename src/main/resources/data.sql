-- Du lieu mau de demo. Moi tai khoan co mat khau: Matkhau@123 (luu dang bcrypt).
-- Khong ghi id co dinh: khoa ngoai lay qua ma (code, username, sku) de chay duoc ca tren H2 lan PostgreSQL.
INSERT INTO warehouse (code, name, type) VALUES
    ('KT',   'Kho tong',    'CENTRAL'),
    ('CN01', 'Chi nhanh 1', 'BRANCH'),
    ('CN02', 'Chi nhanh 2', 'BRANCH');

INSERT INTO app_user (username, password_hash, full_name) VALUES
    ('admin',     '$2a$10$.Rym7pSxPe612dXnYHvAh.K0URVEekUgQw2ZNakZItO9U1GjD9.lK', 'Quan tri he thong'),
    ('thungan1',  '$2a$10$.Rym7pSxPe612dXnYHvAh.K0URVEekUgQw2ZNakZItO9U1GjD9.lK', 'Thu ngan chi nhanh 1'),
    ('thungan1b', '$2a$10$.Rym7pSxPe612dXnYHvAh.K0URVEekUgQw2ZNakZItO9U1GjD9.lK', 'Thu ngan chi nhanh 1 (ca 2)'),
    ('thungan2',  '$2a$10$.Rym7pSxPe612dXnYHvAh.K0URVEekUgQw2ZNakZItO9U1GjD9.lK', 'Thu ngan chi nhanh 2');

INSERT INTO user_assignment (user_id, role, warehouse_id)
    SELECT u.id, 'ADMIN', NULL FROM app_user u WHERE u.username = 'admin';
INSERT INTO user_assignment (user_id, role, warehouse_id)
    SELECT u.id, 'CASHIER', w.id FROM app_user u, warehouse w WHERE u.username IN ('thungan1', 'thungan1b') AND w.code = 'CN01';
INSERT INTO user_assignment (user_id, role, warehouse_id)
    SELECT u.id, 'CASHIER', w.id FROM app_user u, warehouse w WHERE u.username = 'thungan2' AND w.code = 'CN02';

INSERT INTO product (sku, barcode, name, price, min_stock) VALUES
    ('TV-55-A',   '8930000000011', 'Tivi 55 inch',       12990000, 2),
    ('NC-1L',     '8930000000028', 'Noi com dien 1 lit',   890000, 5),
    ('QUAT-DUNG', '8930000000035', 'Quat dung',            650000, 5);

-- Chi nhanh 1 chi con 1 tivi: dung de thu tinh huong hai thu ngan cung ban mon cuoi cung (D02 muc 3.2).
INSERT INTO stock_balance (warehouse_id, product_id, qty)
    SELECT w.id, p.id, 1  FROM warehouse w, product p WHERE w.code = 'CN01' AND p.sku = 'TV-55-A';
INSERT INTO stock_balance (warehouse_id, product_id, qty)
    SELECT w.id, p.id, 10 FROM warehouse w, product p WHERE w.code = 'CN01' AND p.sku = 'NC-1L';
INSERT INTO stock_balance (warehouse_id, product_id, qty)
    SELECT w.id, p.id, 4  FROM warehouse w, product p WHERE w.code = 'CN01' AND p.sku = 'QUAT-DUNG';
INSERT INTO stock_balance (warehouse_id, product_id, qty)
    SELECT w.id, p.id, 3  FROM warehouse w, product p WHERE w.code = 'CN02' AND p.sku = 'TV-55-A';
INSERT INTO stock_balance (warehouse_id, product_id, qty)
    SELECT w.id, p.id, 6  FROM warehouse w, product p WHERE w.code = 'CN02' AND p.sku = 'NC-1L';
