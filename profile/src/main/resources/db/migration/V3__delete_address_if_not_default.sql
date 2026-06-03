CREATE OR REPLACE FUNCTION delete_address_if_not_default(p_address_id BIGINT, p_user_id BIGINT)
RETURNS INTEGER AS $$
DECLARE
    v_default_address_id BIGINT;
    v_deleted INTEGER;
BEGIN
    SELECT default_address_id INTO v_default_address_id
    FROM profiles
    WHERE user_id = p_user_id
    FOR UPDATE;

    IF v_default_address_id IS NOT NULL AND v_default_address_id = p_address_id THEN
        RETURN -1;
    END IF;

    DELETE FROM addresses WHERE id = p_address_id AND user_id = p_user_id;
    GET DIAGNOSTICS v_deleted = ROW_COUNT;
    RETURN v_deleted;
END;
$$ LANGUAGE plpgsql;
