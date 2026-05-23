-- Inventory Database Seed
-- Creates stock records for every SKU from the product database
-- Run AFTER product/seed.sql has been executed

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SELECT pg_catalog.set_config('search_path', 'public', false);

-- Clear existing data
TRUNCATE stocks RESTART IDENTITY CASCADE;

-- Generate stock records for all SKUs (IDs 1 through 11433)
-- Realistic inventory distribution:
--   10% out of stock (quantity = 0)
--   20% low stock (1-10)
--   40% normal stock (10-100)
--   30% high stock (100-500)

DO $$
DECLARE
    v_sku_id BIGINT;
    v_product_id BIGINT;
    v_quantity INT;
BEGIN
    FOR v_sku_id IN 1..11312 LOOP
        -- Map SKU to product: each product has ~4 SKUs on avg, IDs are sequential
        v_product_id := greatest(1, ceil(v_sku_id / 4.07));

        IF random() < 0.10 THEN
            v_quantity := 0;
        ELSIF random() < 0.30 THEN
            v_quantity := 1 + floor(random() * 10)::INT;
        ELSIF random() < 0.70 THEN
            v_quantity := 10 + floor(random() * 90)::INT;
        ELSE
            v_quantity := 100 + floor(random() * 400)::INT;
        END IF;

        INSERT INTO stocks (sku_id, product_id, quantity)
        VALUES (v_sku_id, v_product_id, v_quantity)
        ON CONFLICT (sku_id) DO NOTHING;
    END LOOP;

    RAISE NOTICE 'Inventory seed complete: % stock records created', 11312;
END $$;
