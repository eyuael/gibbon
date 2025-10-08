-- Database setup script for Gibbon DatabaseSink testing
-- Run this script in your PostgreSQL database named 'Sink'

-- Create the events table if it doesn't exist
CREATE TABLE IF NOT EXISTS events (
    id SERIAL PRIMARY KEY,
    event_key TEXT,
    event_value TEXT,
    event_time BIGINT,
    timestamp_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create an index on timestamp for better query performance
CREATE INDEX IF NOT EXISTS idx_events_timestamp ON events(timestamp_ms);
CREATE INDEX IF NOT EXISTS idx_events_event_time ON events(event_time);
CREATE INDEX IF NOT EXISTS idx_events_created_at ON events(created_at);

-- Create a test table with a different schema for advanced testing
CREATE TABLE IF NOT EXISTS test_events (
    id SERIAL PRIMARY KEY,
    event_key TEXT,
    event_value TEXT,
    event_time BIGINT,
    timestamp_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Grant necessary permissions (adjust username as needed)
-- GRANT ALL PRIVILEGES ON TABLE events TO your_username;
-- GRANT ALL PRIVILEGES ON TABLE test_events TO your_username;
-- GRANT USAGE, SELECT ON SEQUENCE events_id_seq TO your_username;
-- GRANT USAGE, SELECT ON SEQUENCE test_events_id_seq TO your_username;

-- Sample queries to verify the setup
-- SELECT COUNT(*) FROM events;
-- SELECT * FROM events ORDER BY created_at DESC LIMIT 10;