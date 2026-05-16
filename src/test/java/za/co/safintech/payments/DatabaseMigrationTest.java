package za.co.safintech.payments;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DatabaseMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayShouldCreateMerchantFoundationTables() {
        List<String> tableNames = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                ORDER BY table_name
                """,
                String.class);

        assertThat(tableNames).contains(
                "flyway_schema_history",
                "merchants",
                "merchant_users",
                "customers",
                "invoices",
                "payment_attempts",
                "idempotency_records",
                "webhook_events",
                "refunds",
                "merchant_balances",
                "settlement_batches",
                "settlement_batch_items",
                "reconciliation_reports",
                "reconciliation_report_items",
                "audit_logs");
    }

    @Test
    void merchantUsersShouldBeMerchantScopedAndEmailUniqueCaseInsensitively() {
        List<String> merchantUserColumns = jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'merchant_users'
                """,
                String.class);

        List<String> merchantUserIndexes = jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'merchant_users'
                """,
                String.class);

        assertThat(merchantUserColumns).contains("merchant_id", "email", "password_hash", "role", "status");
        assertThat(merchantUserIndexes).contains(
                "uk_merchant_users_email_lower",
                "idx_merchant_users_merchant_id",
                "idx_merchant_users_merchant_role");
    }

    @Test
    void merchantsShouldBeLockedToSouthAfricanZarSimulationDefaults() {
        List<String> constraints = jdbcTemplate.queryForList(
                """
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'merchants'
                """,
                String.class);

        assertThat(constraints).contains(
                "chk_merchants_country_code",
                "chk_merchants_default_currency",
                "chk_merchants_type",
                "chk_merchants_status");
    }

    @Test
    void invoicesShouldStoreZarMoneyAndBeMerchantScoped() {
        List<String> invoiceColumns = jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'invoices'
                """,
                String.class);

        List<String> invoiceIndexes = jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'invoices'
                """,
                String.class);

        List<String> invoiceConstraints = jdbcTemplate.queryForList(
                """
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'invoices'
                """,
                String.class);

        assertThat(invoiceColumns).contains("merchant_id", "customer_id", "amount", "currency", "status");
        assertThat(invoiceIndexes).contains(
                "uk_invoices_merchant_invoice_number",
                "idx_invoices_merchant_id",
                "idx_invoices_merchant_status");
        assertThat(invoiceConstraints).contains("fk_invoices_customer_merchant");
    }

    @Test
    void paymentAttemptsShouldStoreSimulatedPaymentStateSafely() {
        List<String> paymentColumns = jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'payment_attempts'
                """,
                String.class);

        List<String> paymentIndexes = jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'payment_attempts'
                """,
                String.class);

        List<String> paymentConstraints = jdbcTemplate.queryForList(
                """
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'payment_attempts'
                """,
                String.class);

        assertThat(paymentColumns).contains(
                "merchant_id",
                "invoice_id",
                "amount",
                "gross_amount",
                "fee_amount",
                "net_amount",
                "currency",
                "payment_method",
                "status",
                "provider_reference");
        assertThat(paymentIndexes).contains(
                "uk_payment_attempts_provider_reference",
                "uk_payment_attempts_invoice_success",
                "idx_payment_attempts_merchant_id",
                "idx_payment_attempts_merchant_status");
        assertThat(paymentConstraints).contains(
                "fk_payment_attempts_invoice_merchant",
                "chk_payment_attempts_gross_amount_positive",
                "chk_payment_attempts_fee_amount_non_negative",
                "chk_payment_attempts_net_amount");
    }

    @Test
    void idempotencyAndWebhookTablesShouldBackRetrySafety() {
        List<String> idempotencyIndexes = jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'idempotency_records'
                """,
                String.class);

        List<String> webhookIndexes = jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'webhook_events'
                """,
                String.class);

        List<String> webhookColumns = jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'webhook_events'
                """,
                String.class);

        assertThat(idempotencyIndexes).contains(
                "uk_idempotency_records_scope_key",
                "idx_idempotency_records_merchant_operation");
        assertThat(webhookIndexes).contains(
                "uk_webhook_events_provider_event_id",
                "idx_webhook_events_provider_reference",
                "idx_webhook_events_target_payment_id");
        assertThat(webhookColumns).contains(
                "provider_event_id",
                "processing_status",
                "raw_payload",
                "requested_payment_status");
    }

    @Test
    void refundsShouldBePaymentLinkedMerchantScopedAndZarOnly() {
        List<String> refundColumns = jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'refunds'
                """,
                String.class);

        List<String> refundIndexes = jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'refunds'
                """,
                String.class);

        List<String> refundConstraints = jdbcTemplate.queryForList(
                """
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'refunds'
                """,
                String.class);

        assertThat(refundColumns).contains(
                "merchant_id",
                "payment_attempt_id",
                "amount",
                "currency",
                "status",
                "provider_reference");
        assertThat(refundIndexes).contains(
                "uk_refunds_provider_reference",
                "idx_refunds_merchant_id",
                "idx_refunds_payment_attempt_id",
                "idx_refunds_merchant_status");
        assertThat(refundConstraints).contains(
                "fk_refunds_payment_merchant",
                "chk_refunds_amount_positive",
                "chk_refunds_currency",
                "chk_refunds_status");
    }

    @Test
    void merchantBalancesShouldStoreMerchantScopedFeeAndRefundTotals() {
        List<String> balanceColumns = jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'merchant_balances'
                """,
                String.class);

        List<String> balanceConstraints = jdbcTemplate.queryForList(
                """
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'merchant_balances'
                """,
                String.class);

        assertThat(balanceColumns).contains(
                "merchant_id",
                "currency",
                "gross_amount",
                "fee_amount",
                "refunded_amount",
                "available_amount",
                "settled_amount");
        assertThat(balanceConstraints).contains(
                "merchant_balances_pkey",
                "chk_merchant_balances_currency",
                "chk_merchant_balances_gross_non_negative",
                "chk_merchant_balances_fee_non_negative",
                "chk_merchant_balances_refunded_non_negative",
                "chk_merchant_balances_settled_non_negative");
    }

    @Test
    void settlementTablesShouldPreserveBatchTotalsAndPreventDoubleSettlement() {
        List<String> batchColumns = jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'settlement_batches'
                """,
                String.class);

        List<String> itemColumns = jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'settlement_batch_items'
                """,
                String.class);

        List<String> itemIndexes = jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'settlement_batch_items'
                """,
                String.class);

        List<String> itemConstraints = jdbcTemplate.queryForList(
                """
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'settlement_batch_items'
                """,
                String.class);

        assertThat(batchColumns).contains(
                "merchant_id",
                "currency",
                "status",
                "gross_amount",
                "fee_amount",
                "refund_amount",
                "net_amount");
        assertThat(itemColumns).contains(
                "settlement_batch_id",
                "merchant_id",
                "payment_attempt_id",
                "gross_amount",
                "fee_amount",
                "refund_amount",
                "net_amount");
        assertThat(itemIndexes).contains(
                "uk_settlement_batch_items_payment_attempt",
                "idx_settlement_batch_items_batch_id",
                "idx_settlement_batch_items_merchant_id");
        assertThat(itemConstraints).contains(
                "fk_settlement_items_payment_merchant",
                "chk_settlement_items_net");
    }

    @Test
    void reconciliationTablesShouldStoreReportsAndMismatchItems() {
        List<String> reportColumns = jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'reconciliation_reports'
                """,
                String.class);

        List<String> itemColumns = jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'reconciliation_report_items'
                """,
                String.class);

        List<String> itemIndexes = jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'reconciliation_report_items'
                """,
                String.class);

        List<String> itemConstraints = jdbcTemplate.queryForList(
                """
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'reconciliation_report_items'
                """,
                String.class);

        assertThat(reportColumns).contains(
                "merchant_id",
                "status",
                "total_records",
                "matched_count",
                "exception_count");
        assertThat(itemColumns).contains(
                "reconciliation_report_id",
                "merchant_id",
                "provider_reference",
                "internal_payment_attempt_id",
                "result_type",
                "internal_amount",
                "external_amount",
                "internal_status",
                "external_status");
        assertThat(itemIndexes).contains(
                "idx_reconciliation_report_items_report_id",
                "idx_reconciliation_report_items_merchant_result",
                "idx_reconciliation_report_items_provider_reference");
        assertThat(itemConstraints).contains(
                "fk_reconciliation_items_payment_merchant",
                "chk_reconciliation_report_items_result_type");
    }
}
