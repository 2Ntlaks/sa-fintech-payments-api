package za.co.safintech.payments;

import org.springframework.boot.SpringApplication;

public class TestSaFintechPaymentsApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(SaFintechPaymentsApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
