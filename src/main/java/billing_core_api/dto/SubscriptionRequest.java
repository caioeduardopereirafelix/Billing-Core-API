package billing_core_api.dto;

import billing_core_api.domain.Plan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubscriptionRequest(@NotBlank
                                  String customerEmail,
                                  @NotBlank
                                  String customerName,
                                  @NotNull
                                  @Positive
                                  Long planId){}
