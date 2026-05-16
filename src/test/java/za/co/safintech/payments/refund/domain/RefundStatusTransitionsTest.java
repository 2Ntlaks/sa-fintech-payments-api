package za.co.safintech.payments.refund.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RefundStatusTransitionsTest {

    @Test
    void shouldAllowRequestedRefundToSucceed() {
        assertThat(RefundStatusTransitions.canMove(RefundStatus.REQUESTED, RefundStatus.SUCCEEDED)).isTrue();
    }

    @Test
    void shouldTreatSucceededRefundAsTerminal() {
        assertThat(RefundStatusTransitions.canMove(RefundStatus.SUCCEEDED, RefundStatus.FAILED)).isFalse();
    }
}
