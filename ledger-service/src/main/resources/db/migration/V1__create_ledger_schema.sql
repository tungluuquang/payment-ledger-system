CREATE TABLE journal_entries (
    journal_entry_id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    correlation_id UUID NOT NULL,
    idempotency_key UUID NOT NULL UNIQUE,
    entry_type VARCHAR(20) NOT NULL,
    original_journal_entry_id UUID,
    debit_account_id UUID NOT NULL,
    credit_account_id UUID NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(1000),
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_journal_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_journal_accounts_differ CHECK (debit_account_id <> credit_account_id),
    CONSTRAINT ck_journal_type CHECK (entry_type IN ('PAYMENT', 'REVERSAL')),
    CONSTRAINT ck_journal_reversal_reference CHECK (
        (entry_type = 'PAYMENT' AND original_journal_entry_id IS NULL)
        OR (entry_type = 'REVERSAL' AND original_journal_entry_id IS NOT NULL)
    ),
    CONSTRAINT fk_journal_original FOREIGN KEY (original_journal_entry_id)
        REFERENCES journal_entries (journal_entry_id),
    CONSTRAINT uk_journal_original_reversal UNIQUE (original_journal_entry_id)
);

CREATE INDEX idx_journal_payment ON journal_entries (payment_id);
CREATE INDEX idx_journal_correlation ON journal_entries (correlation_id);

CREATE TABLE ledger_postings (
    posting_id UUID PRIMARY KEY,
    journal_entry_id UUID NOT NULL,
    account_id UUID NOT NULL,
    side VARCHAR(10) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_posting_journal FOREIGN KEY (journal_entry_id)
        REFERENCES journal_entries (journal_entry_id),
    CONSTRAINT ck_posting_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_posting_side CHECK (side IN ('DEBIT', 'CREDIT')),
    CONSTRAINT uk_posting_journal_side UNIQUE (journal_entry_id, side)
);

CREATE INDEX idx_posting_account_created
    ON ledger_postings (account_id, created_at);

CREATE TABLE processed_commands (
    command_id UUID PRIMARY KEY,
    command_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE ledger_event_outbox (
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
    CONSTRAINT ck_ledger_outbox_status
        CHECK (status IN ('NEW', 'PROCESSING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX idx_ledger_outbox_status_retry
    ON ledger_event_outbox (status, next_retry_at, created_at);
