CREATE OR REPLACE FUNCTION add_cart_item_with_limit(p_cart_id BIGINT, p_sku_id BIGINT, p_quantity INTEGER, p_max_items INTEGER)
RETURNS BIGINT AS $$
DECLARE
    v_item_count INTEGER;
    v_item_id BIGINT;
BEGIN
    PERFORM id FROM carts WHERE id = p_cart_id FOR UPDATE;

    SELECT COUNT(*) INTO v_item_count FROM cart_items WHERE cart_id = p_cart_id;

    IF v_item_count >= p_max_items THEN
        RETURN -1;
    END IF;

    INSERT INTO cart_items (cart_id, sku_id, quantity, created_at, updated_at)
    VALUES (p_cart_id, p_sku_id, p_quantity, NOW(), NOW())
    RETURNING id INTO v_item_id;

    RETURN v_item_id;
END;
$$ LANGUAGE plpgsql;
