ALTER TABLE payment_projections
    ADD COLUMN credit_status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED';

ALTER TABLE payment_projections
    ADD COLUMN credit_transaction_id UUID;

ALTER TABLE payment_projections
    ADD COLUMN credit_reversal_transaction_id UUID;
