CREATE TABLE settlement_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    currency VARCHAR(3) NOT NULL DEFAULT 'ZAR',
    status VARCHAR(40) NOT NULL,
    gross_amount NUMERIC(19, 2) NOT NULL,
    fee_amount NUMERIC(19, 2) NOT NULL,
    refund_amount NUMERIC(19, 2) NOT NULL,
    net_amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_settlement_batches_currency CHECK (currency = 'ZAR'),
    CONSTRAINT chk_settlement_batches_status CHECK (
        status IN (
            'CREATED',
            'PROCESSING',
            'SETTLED',
            'FAILED',
            'CANCELLED'
        )
    ),
    CONSTRAINT chk_settlement_batches_gross_non_negative CHECK (gross_amount >= 0),
    CONSTRAINT chk_settlement_batches_fee_non_negative CHECK (fee_amount >= 0),
    CONSTRAINT chk_settlement_batches_refund_non_negative CHECK (refund_amount >= 0)
);

CREATE TABLE settlement_batch_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_batch_id UUID NOT NULL REFERENCES settlement_batches(id),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    payment_attempt_id UUID NOT NULL,
    gross_amount NUMERIC(19, 2) NOT NULL,
    fee_amount NUMERIC(19, 2) NOT NULL,
    refund_amount NUMERIC(19, 2) NOT NULL,
    net_amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_settlement_items_payment_merchant
        FOREIGN KEY (payment_attempt_id, merchant_id)
        REFERENCES payment_attempts(id, merchant_id),
    CONSTRAINT chk_settlement_items_gross_positive CHECK (gross_amount > 0),
    CONSTRAINT chk_settlement_items_fee_non_negative CHECK (fee_amount >= 0),
    CONSTRAINT chk_settlement_items_refund_non_negative CHECK (refund_amount >= 0),
    CONSTRAINT chk_settlement_items_net CHECK (net_amount = gross_amount - fee_amount - refund_amount)
);

CREATE UNIQUE INDEX uk_settlement_batch_items_payment_attempt
    ON settlement_batch_items(payment_attempt_id);

CREATE INDEX idx_settlement_batches_merchant_id
    ON settlement_batches(merchant_id);

CREATE INDEX idx_settlement_batches_merchant_status
    ON settlement_batches(merchant_id, status);

CREATE INDEX idx_settlement_batch_items_batch_id
    ON settlement_batch_items(settlement_batch_id);

CREATE INDEX idx_settlement_batch_items_merchant_id
    ON settlement_batch_items(merchant_id);
