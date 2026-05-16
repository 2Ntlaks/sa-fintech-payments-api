package za.co.safintech.payments.balance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class FeeCalculator {

    private static final BigDecimal PERCENTAGE_RATE = new BigDecimal("0.029");
    private static final BigDecimal FIXED_FEE = new BigDecimal("1.00");

    public BigDecimal calculateFee(BigDecimal grossAmount) {
        return grossAmount
                .multiply(PERCENTAGE_RATE)
                .add(FIXED_FEE)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
