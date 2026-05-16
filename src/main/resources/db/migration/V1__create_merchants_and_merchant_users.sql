CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE merchants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_name VARCHAR(160) NOT NULL,
    trading_name VARCHAR(160),
    registration_number VARCHAR(80),
    merchant_type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    country_code CHAR(2) NOT NULL DEFAULT 'ZA',
    default_currency CHAR(3) NOT NULL DEFAULT 'ZAR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_merchants_country_code CHECK (country_code = 'ZA'),
    CONSTRAINT chk_merchants_default_currency CHECK (default_currency = 'ZAR'),
    CONSTRAINT chk_merchants_type CHECK (
        merchant_type IN (
            'SPAZA_SHOP',
            'TUTORING_BUSINESS',
            'ONLINE_COURSE_SELLER',
            'SMALL_RETAILER',
            'OTHER'
        )
    ),
    CONSTRAINT chk_merchants_status CHECK (
        status IN (
            'PENDING_VERIFICATION',
            'ACTIVE',
            'SUSPENDED',
            'CLOSED'
        )
    )
);

CREATE INDEX idx_merchants_status ON merchants(status);
CREATE INDEX idx_merchants_type ON merchants(merchant_type);

CREATE TABLE merchant_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_merchant_users_role CHECK (
        role IN (
            'OWNER',
            'FINANCE',
            'SUPPORT',
            'VIEWER'
        )
    ),
    CONSTRAINT chk_merchant_users_status CHECK (
        status IN (
            'ACTIVE',
            'DISABLED',
            'INVITED'
        )
    )
);

CREATE UNIQUE INDEX uk_merchant_users_email_lower ON merchant_users(lower(email));
CREATE INDEX idx_merchant_users_merchant_id ON merchant_users(merchant_id);
CREATE INDEX idx_merchant_users_merchant_role ON merchant_users(merchant_id, role);
CREATE INDEX idx_merchant_users_status ON merchant_users(status);
