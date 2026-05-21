ALTER TABLE skus DROP COLUMN is_default;
ALTER TABLE products ADD COLUMN default_sku_id BIGINT REFERENCES skus(id);
