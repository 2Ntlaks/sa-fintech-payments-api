package za.co.safintech.payments.balance.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.safintech.payments.balance.domain.MerchantBalance;
import za.co.safintech.payments.balance.dto.MerchantBalanceResponse;
import za.co.safintech.payments.balance.repository.MerchantBalanceRepository;

@Service
@Profile("!local")
public class MerchantBalanceService {

    private final MerchantBalanceRepository merchantBalanceRepository;

    public MerchantBalanceService(MerchantBalanceRepository merchantBalanceRepository) {
        this.merchantBalanceRepository = merchantBalanceRepository;
    }

    @Transactional
    public void applySuccessfulPayment(UUID merchantId, BigDecimal grossAmount, BigDecimal feeAmount, BigDecimal netAmount) {
        MerchantBalance balance = lockedBalanceFor(merchantId);
        balance.applySuccessfulPayment(grossAmount, feeAmount, netAmount);
        merchantBalanceRepository.save(balance);
    }

    @Transactional
    public void applySuccessfulRefund(UUID merchantId, BigDecimal refundAmount) {
        MerchantBalance balance = lockedBalanceFor(merchantId);
        balance.applySuccessfulRefund(refundAmount);
        merchantBalanceRepository.save(balance);
    }

    @Transactional
    public void applySettlement(UUID merchantId, BigDecimal settlementAmount) {
        MerchantBalance balance = lockedBalanceFor(merchantId);
        balance.applySettlement(settlementAmount);
        merchantBalanceRepository.save(balance);
    }

    @Transactional(readOnly = true)
    public MerchantBalanceResponse getBalance(UUID merchantId) {
        return merchantBalanceRepository.findById(merchantId)
                .map(this::toResponse)
                .orElseGet(() -> toResponse(new MerchantBalance(merchantId)));
    }

    private MerchantBalance lockedBalanceFor(UUID merchantId) {
        return merchantBalanceRepository.findWithLockByMerchantId(merchantId)
                .orElseGet(() -> new MerchantBalance(merchantId));
    }

    private MerchantBalanceResponse toResponse(MerchantBalance balance) {
        return new MerchantBalanceResponse(
                balance.merchantId(),
                balance.currency(),
                balance.grossAmount(),
                balance.feeAmount(),
                balance.refundedAmount(),
                balance.availableAmount(),
                balance.settledAmount());
    }
}
