ALTER TABLE payment_attempts
    DROP CONSTRAINT chk_payment_attempts_status;

ALTER TABLE payment_attempts
    ADD CONSTRAINT chk_payment_attempts_status CHECK (
        status IN (
            'CREATED',
            'PENDING',
            'PROCESSING',
            'SUCCEEDED',
            'FAILED',
            'CANCELLED',
            'EXPIRED',
            'PARTIALLY_REFUNDED',
            'REFUNDED'
        )
    );

ALTER TABLE payment_attempts
    ADD CONSTRAINT uk_payment_attempts_id_merchant UNIQUE (id, merchant_id);

DROP INDEX uk_payment_attempts_invoice_success;

CREATE UNIQUE INDEX uk_payment_attempts_invoice_success
    ON payment_attempts(invoice_id)
    WHERE status IN ('SUCCEEDED', 'PARTIALLY_REFUNDED', 'REFUNDED');

CREATE TABLE refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    payment_attempt_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'ZAR',
    status VARCHAR(40) NOT NULL,
    provider_reference VARCHAR(64) NOT NULL,
    reason VARCHAR(255),
    status_changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_refunds_payment_merchant
        FOREIGN KEY (payment_attempt_id, merchant_id)
        REFERENCES payment_attempts(id, merchant_id),
    CONSTRAINT chk_refunds_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_refunds_currency CHECK (currency = 'ZAR'),
    CONSTRAINT chk_refunds_status CHECK (
        status IN (
            'REQUESTED',
            'PROCESSING',
            'SUCCEEDED',
            'FAILED',
            'CANCELLED'
        )
    )
);

CREATE UNIQUE INDEX uk_refunds_provider_reference ON refunds(provider_reference);
CREATE INDEX idx_refunds_merchant_id ON refunds(merchant_id);
CREATE INDEX idx_refunds_payment_attempt_id ON refunds(payment_attempt_id);
CREATE INDEX idx_refunds_merchant_status ON refunds(merchant_id, status);
