ALTER TABLE skus DROP COLUMN primary_variant_key;
ALTER TABLE products ADD COLUMN primary_variant_key VARCHAR(255);
