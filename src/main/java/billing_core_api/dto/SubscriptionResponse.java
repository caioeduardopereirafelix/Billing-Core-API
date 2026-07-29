package billing_core_api.dto;

import billing_core_api.domain.BillingCycle;

import java.math.BigDecimal;

public record SubscriptionResponse(String customerEmail,
                                   String customerName) {
}
