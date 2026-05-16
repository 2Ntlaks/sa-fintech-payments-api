package za.co.safintech.payments.webhook.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.safintech.payments.webhook.domain.WebhookEvent;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    Optional<WebhookEvent> findByProviderEventId(String providerEventId);
}
