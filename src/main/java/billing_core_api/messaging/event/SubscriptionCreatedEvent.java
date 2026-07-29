package billing_core_api.messaging.event;

public record SubscriptionCreatedEvent(Long subscriptionId,
                                       String customerEmail,
                                       String customerName,
                                       String planName,
                                       String correlationID) {
}
