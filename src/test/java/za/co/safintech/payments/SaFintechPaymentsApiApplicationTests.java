package za.co.safintech.payments;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;

import za.co.safintech.payments.auth.repository.MerchantUserRepository;
import za.co.safintech.payments.balance.repository.MerchantBalanceRepository;
import za.co.safintech.payments.customer.repository.CustomerRepository;
import za.co.safintech.payments.invoice.repository.InvoiceRepository;
import za.co.safintech.payments.merchant.repository.MerchantRepository;
import za.co.safintech.payments.payment.repository.IdempotencyRecordRepository;
import za.co.safintech.payments.payment.repository.PaymentAttemptRepository;
import za.co.safintech.payments.audit.repository.AuditLogRepository;
import za.co.safintech.payments.refund.repository.RefundRepository;
import za.co.safintech.payments.reconciliation.repository.ReconciliationReportItemRepository;
import za.co.safintech.payments.reconciliation.repository.ReconciliationReportRepository;
import za.co.safintech.payments.settlement.repository.SettlementBatchItemRepository;
import za.co.safintech.payments.settlement.repository.SettlementBatchRepository;
import za.co.safintech.payments.webhook.repository.WebhookEventRepository;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class SaFintechPaymentsApiApplicationTests {

    @MockBean
    private MerchantRepository merchantRepository;

    @MockBean
    private MerchantUserRepository merchantUserRepository;

    @MockBean
    private CustomerRepository customerRepository;

    @MockBean
    private InvoiceRepository invoiceRepository;

    @MockBean
    private PaymentAttemptRepository paymentAttemptRepository;

    @MockBean
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @MockBean
    private RefundRepository refundRepository;

    @MockBean
    private WebhookEventRepository webhookEventRepository;

    @MockBean
    private MerchantBalanceRepository merchantBalanceRepository;

    @MockBean
    private SettlementBatchRepository settlementBatchRepository;

    @MockBean
    private SettlementBatchItemRepository settlementBatchItemRepository;

    @MockBean
    private ReconciliationReportRepository reconciliationReportRepository;

    @MockBean
    private ReconciliationReportItemRepository reconciliationReportItemRepository;

    @Test
    void contextLoadsWithoutDatabaseForFoundationMilestone() {
    }

}
