ALTER TABLE stocks ADD COLUMN available_count INTEGER NOT NULL DEFAULT 0;
UPDATE stocks SET available_count = quantity;
