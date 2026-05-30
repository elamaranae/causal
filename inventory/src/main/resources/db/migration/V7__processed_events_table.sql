CREATE TABLE processed_events (
    event_id VARCHAR(255) NOT NULL,
    consumer_id VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, consumer_id)
);
