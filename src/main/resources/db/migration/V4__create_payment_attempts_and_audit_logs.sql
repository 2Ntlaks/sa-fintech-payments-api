ALTER TABLE invoices
    ADD CONSTRAINT uk_invoices_id_merchant UNIQUE (id, merchant_id);

CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    invoice_id UUID NOT NULL,
    provider_reference VARCHAR(64) NOT NULL,
    payment_method VARCHAR(40) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'ZAR',
    status VARCHAR(40) NOT NULL,
    failure_reason VARCHAR(255),
    status_changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_payment_attempts_invoice_merchant
        FOREIGN KEY (invoice_id, merchant_id)
        REFERENCES invoices(id, merchant_id),
    CONSTRAINT chk_payment_attempts_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_payment_attempts_currency CHECK (currency = 'ZAR'),
    CONSTRAINT chk_payment_attempts_method CHECK (
        payment_method IN (
            'CARD_SIMULATED',
            'EFT_SIMULATED',
            'PAYSHAP_SIMULATED',
            'DEBIT_ORDER_SIMULATED'
        )
    ),
    CONSTRAINT chk_payment_attempts_status CHECK (
        status IN (
            'CREATED',
            'PENDING',
            'PROCESSING',
            'SUCCEEDED',
            'FAILED',
            'CANCELLED',
            'EXPIRED'
        )
    )
);

CREATE UNIQUE INDEX uk_payment_attempts_provider_reference ON payment_attempts(provider_reference);
CREATE UNIQUE INDEX uk_payment_attempts_invoice_success ON payment_attempts(invoice_id) WHERE status = 'SUCCEEDED';
CREATE INDEX idx_payment_attempts_merchant_id ON payment_attempts(merchant_id);
CREATE INDEX idx_payment_attempts_invoice_id ON payment_attempts(invoice_id);
CREATE INDEX idx_payment_attempts_merchant_status ON payment_attempts(merchant_id, status);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID REFERENCES merchants(id),
    actor_type VARCHAR(40) NOT NULL,
    actor_id UUID,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_id UUID NOT NULL,
    previous_state VARCHAR(80),
    new_state VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_merchant_id ON audit_logs(merchant_id);
CREATE INDEX idx_audit_logs_target ON audit_logs(target_type, target_id);
