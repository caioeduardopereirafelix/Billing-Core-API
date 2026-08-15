package billing_core_api.dto.subscription;

public record SubscriptionResponse(String customerEmail,
                                   String customerName,
                                   String planName,
                                   String status) {
}
