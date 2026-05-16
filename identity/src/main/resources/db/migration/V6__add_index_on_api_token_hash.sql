CREATE UNIQUE INDEX idx_users_api_token_hash ON users (api_token_hash) WHERE api_token_hash IS NOT NULL;
