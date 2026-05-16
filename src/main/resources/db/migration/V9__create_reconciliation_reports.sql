CREATE TABLE reconciliation_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    status VARCHAR(40) NOT NULL,
    total_records INTEGER NOT NULL,
    matched_count INTEGER NOT NULL,
    exception_count INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_reconciliation_reports_status CHECK (
        status IN (
            'COMPLETED'
        )
    ),
    CONSTRAINT chk_reconciliation_reports_total_records_non_negative CHECK (total_records >= 0),
    CONSTRAINT chk_reconciliation_reports_matched_count_non_negative CHECK (matched_count >= 0),
    CONSTRAINT chk_reconciliation_reports_exception_count_non_negative CHECK (exception_count >= 0)
);

CREATE TABLE reconciliation_report_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reconciliation_report_id UUID NOT NULL REFERENCES reconciliation_reports(id),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    provider_reference VARCHAR(64) NOT NULL,
    internal_payment_attempt_id UUID,
    result_type VARCHAR(40) NOT NULL,
    internal_amount NUMERIC(19, 2),
    external_amount NUMERIC(19, 2),
    internal_currency VARCHAR(3),
    external_currency VARCHAR(3),
    internal_status VARCHAR(40),
    external_status VARCHAR(40),
    details VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_reconciliation_items_payment_merchant
        FOREIGN KEY (internal_payment_attempt_id, merchant_id)
        REFERENCES payment_attempts(id, merchant_id),
    CONSTRAINT chk_reconciliation_report_items_result_type CHECK (
        result_type IN (
            'MATCHED',
            'MISSING_INTERNAL',
            'MISSING_EXTERNAL',
            'AMOUNT_MISMATCH',
            'STATUS_MISMATCH',
            'DUPLICATE_EXTERNAL_REFERENCE'
        )
    )
);

CREATE INDEX idx_reconciliation_reports_merchant_id
    ON reconciliation_reports(merchant_id);

CREATE INDEX idx_reconciliation_report_items_report_id
    ON reconciliation_report_items(reconciliation_report_id);

CREATE INDEX idx_reconciliation_report_items_merchant_result
    ON reconciliation_report_items(merchant_id, result_type);

CREATE INDEX idx_reconciliation_report_items_provider_reference
    ON reconciliation_report_items(provider_reference);
