-- orders: add idempotency_key, total_currency, and address fields
ALTER TABLE orders ADD COLUMN idempotency_key VARCHAR(255);
ALTER TABLE orders ADD COLUMN total_currency VARCHAR(3) NOT NULL DEFAULT 'INR';
ALTER TABLE orders ADD COLUMN address_label VARCHAR(100);
ALTER TABLE orders ADD COLUMN address_line1 VARCHAR(255);
ALTER TABLE orders ADD COLUMN address_line2 VARCHAR(255);
ALTER TABLE orders ADD COLUMN address_city VARCHAR(100);
ALTER TABLE orders ADD COLUMN address_state VARCHAR(100);
ALTER TABLE orders ADD COLUMN address_country VARCHAR(100);
ALTER TABLE orders ADD COLUMN address_pincode VARCHAR(20);
ALTER TABLE orders ADD COLUMN address_phone_number VARCHAR(50);
ALTER TABLE orders ADD CONSTRAINT uq_orders_idempotency_key UNIQUE (idempotency_key);

-- order_items: add delivery_status, purchase_amount/currency, sku_name/description, unique constraint
ALTER TABLE order_items ADD COLUMN delivery_status VARCHAR(50) NOT NULL DEFAULT 'PENDING';
ALTER TABLE order_items ADD COLUMN purchase_amount NUMERIC(12,2);
ALTER TABLE order_items ADD COLUMN purchase_currency VARCHAR(3) NOT NULL DEFAULT 'INR';
ALTER TABLE order_items ADD COLUMN sku_name VARCHAR(255);
ALTER TABLE order_items ADD COLUMN sku_description TEXT;
ALTER TABLE order_items DROP COLUMN price;
ALTER TABLE order_items ADD CONSTRAINT uq_order_items_order_sku UNIQUE (order_id, sku_id);
