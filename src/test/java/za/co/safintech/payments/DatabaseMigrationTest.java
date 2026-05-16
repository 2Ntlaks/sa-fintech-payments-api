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

        assertThat(tableNames).contains("flyway_schema_history", "merchants", "merchant_users", "customers", "invoices");
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
}
