ALTER TABLE stocks ADD CONSTRAINT chk_quantity_positive CHECK (quantity >= 0);
ALTER TABLE stocks ADD CONSTRAINT chk_available_count_positive CHECK (available_count >= 0);
