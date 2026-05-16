package za.co.safintech.payments.balance.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class FeeCalculatorTest {

    private final FeeCalculator feeCalculator = new FeeCalculator();

    @Test
    void shouldCalculateSimplePlatformFeeWithHalfUpRounding() {
        BigDecimal fee = feeCalculator.calculateFee(new BigDecimal("125.50"));

        assertThat(fee).isEqualByComparingTo("4.64");
    }

    @Test
    void shouldRoundHalfUpToTwoDecimalPlaces() {
        BigDecimal fee = feeCalculator.calculateFee(new BigDecimal("10.50"));

        assertThat(fee).isEqualByComparingTo("1.30");
    }
}
