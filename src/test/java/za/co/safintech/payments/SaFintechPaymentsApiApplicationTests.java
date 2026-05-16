package za.co.safintech.payments;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;

import za.co.safintech.payments.auth.repository.MerchantUserRepository;
import za.co.safintech.payments.customer.repository.CustomerRepository;
import za.co.safintech.payments.invoice.repository.InvoiceRepository;
import za.co.safintech.payments.merchant.repository.MerchantRepository;

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

    @Test
    void contextLoadsWithoutDatabaseForFoundationMilestone() {
    }

}
