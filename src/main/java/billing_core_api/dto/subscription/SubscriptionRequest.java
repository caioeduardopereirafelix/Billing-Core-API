package billing_core_api.dto.subscription;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubscriptionRequest(@NotNull
                                  @Positive
                                  Long planId) {}
