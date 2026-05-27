ALTER TABLE reservations ADD COLUMN user_id BIGINT;
CREATE INDEX idx_reservations_user_id ON reservations (user_id);
