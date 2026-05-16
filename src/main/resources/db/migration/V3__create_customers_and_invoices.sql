CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(320),
    phone_number VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_customers_phone_number_za CHECK (phone_number ~ '^\+27[0-9]{9}$'),
    CONSTRAINT uk_customers_id_merchant UNIQUE (id, merchant_id)
);

CREATE INDEX idx_customers_merchant_id ON customers(merchant_id);
CREATE INDEX idx_customers_merchant_phone ON customers(merchant_id, phone_number);

CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    customer_id UUID NOT NULL,
    invoice_number VARCHAR(40) NOT NULL,
    description VARCHAR(255),
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'ZAR',
    status VARCHAR(40) NOT NULL,
    due_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_invoices_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_invoices_currency CHECK (currency = 'ZAR'),
    CONSTRAINT chk_invoices_status CHECK (
        status IN (
            'ISSUED',
            'PAID',
            'EXPIRED',
            'CANCELLED',
            'PARTIALLY_REFUNDED',
            'REFUNDED'
        )
    ),
    CONSTRAINT fk_invoices_customer_merchant
        FOREIGN KEY (customer_id, merchant_id)
        REFERENCES customers(id, merchant_id)
);

CREATE UNIQUE INDEX uk_invoices_merchant_invoice_number ON invoices(merchant_id, invoice_number);
CREATE INDEX idx_invoices_merchant_id ON invoices(merchant_id);
CREATE INDEX idx_invoices_merchant_status ON invoices(merchant_id, status);
CREATE INDEX idx_invoices_customer_id ON invoices(customer_id);
