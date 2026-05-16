ALTER TABLE payment_attempts
    ADD COLUMN gross_amount NUMERIC(19, 2),
    ADD COLUMN fee_amount NUMERIC(19, 2),
    ADD COLUMN net_amount NUMERIC(19, 2);

UPDATE payment_attempts
SET gross_amount = amount,
    fee_amount = 0.00,
    net_amount = amount;

ALTER TABLE payment_attempts
    ALTER COLUMN gross_amount SET NOT NULL,
    ALTER COLUMN fee_amount SET NOT NULL,
    ALTER COLUMN net_amount SET NOT NULL,
    ADD CONSTRAINT chk_payment_attempts_gross_amount_positive CHECK (gross_amount > 0),
    ADD CONSTRAINT chk_payment_attempts_fee_amount_non_negative CHECK (fee_amount >= 0),
    ADD CONSTRAINT chk_payment_attempts_net_amount CHECK (net_amount = gross_amount - fee_amount);

CREATE TABLE merchant_balances (
    merchant_id UUID PRIMARY KEY REFERENCES merchants(id),
    currency VARCHAR(3) NOT NULL DEFAULT 'ZAR',
    gross_amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    fee_amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    refunded_amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    available_amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    settled_amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_merchant_balances_currency CHECK (currency = 'ZAR'),
    CONSTRAINT chk_merchant_balances_gross_non_negative CHECK (gross_amount >= 0),
    CONSTRAINT chk_merchant_balances_fee_non_negative CHECK (fee_amount >= 0),
    CONSTRAINT chk_merchant_balances_refunded_non_negative CHECK (refunded_amount >= 0),
    CONSTRAINT chk_merchant_balances_settled_non_negative CHECK (settled_amount >= 0)
);
