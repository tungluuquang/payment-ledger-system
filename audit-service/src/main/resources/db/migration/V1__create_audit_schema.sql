CREATE TABLE audit_events (
    event_id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    correlation_id UUID,
    event_type VARCHAR(150) NOT NULL,
    source_topic VARCHAR(255) NOT NULL,
    source_partition INTEGER NOT NULL,
    source_offset BIGINT NOT NULL,
    trace_id VARCHAR(64),
    span_id VARCHAR(32),
    payload TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_audit_kafka_position UNIQUE (
        source_topic,
        source_partition,
        source_offset
    )
);

CREATE INDEX idx_audit_payment_occurred
    ON audit_events (payment_id, occurred_at);
CREATE INDEX idx_audit_correlation_occurred
    ON audit_events (correlation_id, occurred_at);
CREATE INDEX idx_audit_trace
    ON audit_events (trace_id);
CREATE INDEX idx_audit_type_occurred
    ON audit_events (event_type, occurred_at);
