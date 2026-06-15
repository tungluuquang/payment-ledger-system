CREATE TABLE payment_analytics (
    payment_id UUID PRIMARY KEY,
    correlation_id UUID,
    source_account_id UUID,
    destination_account_id UUID,
    amount NUMERIC(19, 4),
    currency VARCHAR(3),
    status VARCHAR(20) NOT NULL,
    failure_stage VARCHAR(50),
    failure_code VARCHAR(100),
    failure_reason VARCHAR(1000),
    initiated_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    last_event_type VARCHAR(150) NOT NULL,
    last_event_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_analytics_payment_status CHECK (
        status IN ('INITIATED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')
    )
);

CREATE INDEX idx_payment_analytics_initiated
    ON payment_analytics (initiated_at);
CREATE INDEX idx_payment_analytics_status
    ON payment_analytics (status, updated_at);

CREATE TABLE analytics_events (
    event_id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    correlation_id UUID,
    event_type VARCHAR(150) NOT NULL,
    event_category VARCHAR(50) NOT NULL,
    error_code VARCHAR(100),
    reason VARCHAR(1000),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_analytics_events_occurred
    ON analytics_events (occurred_at);
CREATE INDEX idx_analytics_events_category_occurred
    ON analytics_events (event_category, occurred_at);
CREATE INDEX idx_analytics_events_payment
    ON analytics_events (payment_id, occurred_at);
