CREATE TABLE projection_payment_locks (
    payment_id UUID PRIMARY KEY
);

CREATE TABLE payment_projections (
    payment_id UUID PRIMARY KEY,
    correlation_id UUID,
    source_account_id UUID,
    destination_account_id UUID,
    amount NUMERIC(19, 4),
    currency VARCHAR(3),
    payment_status VARCHAR(20) NOT NULL,
    fraud_status VARCHAR(20) NOT NULL,
    debit_status VARCHAR(20) NOT NULL,
    ledger_status VARCHAR(20) NOT NULL,
    reversal_status VARCHAR(20) NOT NULL,
    debit_transaction_id UUID,
    reversal_transaction_id UUID,
    journal_entry_id UUID,
    reversal_journal_entry_id UUID,
    last_event_type VARCHAR(150) NOT NULL,
    last_error VARCHAR(1000),
    initiated_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    last_event_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_projection_status_updated
    ON payment_projections (payment_status, updated_at DESC);
CREATE INDEX idx_projection_source_updated
    ON payment_projections (source_account_id, updated_at DESC);
CREATE INDEX idx_projection_destination_updated
    ON payment_projections (destination_account_id, updated_at DESC);
CREATE INDEX idx_projection_correlation
    ON payment_projections (correlation_id);

CREATE TABLE projection_events (
    event_id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    correlation_id UUID,
    event_type VARCHAR(150) NOT NULL,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_projection_event_payment_time
    ON projection_events (payment_id, occurred_at, processed_at);
