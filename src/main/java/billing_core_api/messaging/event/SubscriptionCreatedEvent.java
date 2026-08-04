package billing_core_api.messaging.event;

import java.util.UUID;

public record SubscriptionCreatedEvent(UUID eventId,
                                       Long subscriptionId,
                                       String customerEmail,
                                       String customerName,
                                       String planName,
                                       String correlationID) {
}
