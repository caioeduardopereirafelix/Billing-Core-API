package billing_core_api.dto;

import billing_core_api.domain.BillingCycle;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record PlanRequest(@NotBlank
                          String namePlan,
                          @NotBlank
                          String description,
                          @NotBlank
                          BigDecimal price,
                          @NotBlank
                          BillingCycle billingCycle) {
}
