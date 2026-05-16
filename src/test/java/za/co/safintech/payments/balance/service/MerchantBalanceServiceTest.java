package za.co.safintech.payments.balance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.safintech.payments.balance.domain.MerchantBalance;
import za.co.safintech.payments.balance.repository.MerchantBalanceRepository;

@ExtendWith(MockitoExtension.class)
class MerchantBalanceServiceTest {

    @Mock
    private MerchantBalanceRepository merchantBalanceRepository;

    private MerchantBalanceService merchantBalanceService;

    @BeforeEach
    void setUp() {
        merchantBalanceService = new MerchantBalanceService(merchantBalanceRepository);
    }

    @Test
    void shouldCreateBalanceAndApplySuccessfulPaymentAmounts() {
        UUID merchantId = UUID.randomUUID();

        when(merchantBalanceRepository.findWithLockByMerchantId(merchantId)).thenReturn(Optional.empty());
        when(merchantBalanceRepository.save(any(MerchantBalance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        merchantBalanceService.applySuccessfulPayment(
                merchantId,
                new BigDecimal("125.50"),
                new BigDecimal("4.64"),
                new BigDecimal("120.86"));

        ArgumentCaptor<MerchantBalance> balanceCaptor = ArgumentCaptor.forClass(MerchantBalance.class);
        verify(merchantBalanceRepository).save(balanceCaptor.capture());
        MerchantBalance savedBalance = balanceCaptor.getValue();

        assertThat(savedBalance.merchantId()).isEqualTo(merchantId);
        assertThat(savedBalance.grossAmount()).isEqualByComparingTo("125.50");
        assertThat(savedBalance.feeAmount()).isEqualByComparingTo("4.64");
        assertThat(savedBalance.availableAmount()).isEqualByComparingTo("120.86");
        assertThat(savedBalance.refundedAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldReduceAvailableBalanceWhenRefundSucceeds() {
        UUID merchantId = UUID.randomUUID();
        MerchantBalance balance = new MerchantBalance(merchantId);
        balance.applySuccessfulPayment(new BigDecimal("125.50"), new BigDecimal("4.64"), new BigDecimal("120.86"));

        when(merchantBalanceRepository.findWithLockByMerchantId(merchantId)).thenReturn(Optional.of(balance));
        when(merchantBalanceRepository.save(any(MerchantBalance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        merchantBalanceService.applySuccessfulRefund(merchantId, new BigDecimal("50.00"));

        assertThat(balance.grossAmount()).isEqualByComparingTo("125.50");
        assertThat(balance.feeAmount()).isEqualByComparingTo("4.64");
        assertThat(balance.refundedAmount()).isEqualByComparingTo("50.00");
        assertThat(balance.availableAmount()).isEqualByComparingTo("70.86");
    }

    @Test
    void shouldMoveAvailableBalanceToSettledWhenSettlementIsCreated() {
        UUID merchantId = UUID.randomUUID();
        MerchantBalance balance = new MerchantBalance(merchantId);
        balance.applySuccessfulPayment(new BigDecimal("125.50"), new BigDecimal("4.64"), new BigDecimal("120.86"));

        when(merchantBalanceRepository.findWithLockByMerchantId(merchantId)).thenReturn(Optional.of(balance));
        when(merchantBalanceRepository.save(any(MerchantBalance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        merchantBalanceService.applySettlement(merchantId, new BigDecimal("120.86"));

        assertThat(balance.availableAmount()).isEqualByComparingTo("0.00");
        assertThat(balance.settledAmount()).isEqualByComparingTo("120.86");
    }

    @Test
    void shouldReturnZeroBalanceWhenMerchantHasNoBalanceRecordYet() {
        UUID merchantId = UUID.randomUUID();

        when(merchantBalanceRepository.findById(merchantId)).thenReturn(Optional.empty());

        var response = merchantBalanceService.getBalance(merchantId);

        assertThat(response.merchantId()).isEqualTo(merchantId);
        assertThat(response.currency()).isEqualTo("ZAR");
        assertThat(response.grossAmount()).isEqualByComparingTo("0.00");
        assertThat(response.feeAmount()).isEqualByComparingTo("0.00");
        assertThat(response.refundedAmount()).isEqualByComparingTo("0.00");
        assertThat(response.availableAmount()).isEqualByComparingTo("0.00");
        assertThat(response.settledAmount()).isEqualByComparingTo("0.00");
    }
}
