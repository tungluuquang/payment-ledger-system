CREATE TABLE fraud_account_locks (
    account_id UUID PRIMARY KEY
);

CREATE TABLE fraud_decisions (
    decision_id UUID PRIMARY KEY,
    payment_id UUID NOT NULL UNIQUE,
    account_id UUID NOT NULL,
    correlation_id UUID NOT NULL,
    idempotency_key UUID NOT NULL UNIQUE,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    rule_code VARCHAR(100) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    rule_version VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_fraud_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_fraud_decision_status
        CHECK (status IN ('PASSED', 'FAILED'))
);

CREATE INDEX idx_fraud_decision_account_created
    ON fraud_decisions (account_id, created_at);
CREATE INDEX idx_fraud_decision_correlation
    ON fraud_decisions (correlation_id);

CREATE TABLE processed_commands (
    command_id UUID PRIMARY KEY,
    command_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE fraud_event_outbox (
    event_id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    topic VARCHAR(255) NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processing_started_at TIMESTAMP WITH TIME ZONE,
    published_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_fraud_outbox_status
        CHECK (status IN ('NEW', 'PROCESSING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX idx_fraud_outbox_status_retry
    ON fraud_event_outbox (status, next_retry_at, created_at);
