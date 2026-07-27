package billing_core_api.dto;

import billing_core_api.domain.Plan;
import jakarta.validation.constraints.NotBlank;

public record SubscriptionRequest(@NotBlank
                                  String customerEmail,
                                  @NotBlank
                                  String customerName,
                                  @NotBlank
                                  Long planId,
                                  @NotBlank
                                  String correlationId){}
