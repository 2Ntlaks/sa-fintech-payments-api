CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    operation VARCHAR(80) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_idempotency_records_scope_key UNIQUE (merchant_id, operation, idempotency_key)
);

CREATE INDEX idx_idempotency_records_merchant_operation
    ON idempotency_records(merchant_id, operation);

CREATE TABLE webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID REFERENCES merchants(id),
    provider_event_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    provider_reference VARCHAR(64),
    target_payment_id UUID REFERENCES payment_attempts(id),
    requested_payment_status VARCHAR(40),
    processing_status VARCHAR(40) NOT NULL,
    raw_payload TEXT NOT NULL,
    failure_reason VARCHAR(255),
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_webhook_events_provider_event_id UNIQUE (provider_event_id),
    CONSTRAINT chk_webhook_events_processing_status CHECK (
        processing_status IN (
            'RECEIVED',
            'PROCESSED',
            'IGNORED_DUPLICATE',
            'IGNORED_OUT_OF_ORDER',
            'FAILED_VALIDATION',
            'FAILED_PROCESSING'
        )
    )
);

CREATE INDEX idx_webhook_events_merchant_id ON webhook_events(merchant_id);
CREATE INDEX idx_webhook_events_provider_reference ON webhook_events(provider_reference);
CREATE INDEX idx_webhook_events_target_payment_id ON webhook_events(target_payment_id);
