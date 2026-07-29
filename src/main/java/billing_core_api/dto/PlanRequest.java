package billing_core_api.dto;

import billing_core_api.domain.BillingCycle;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PlanRequest(@NotBlank
                          String namePlan,
                          @NotBlank
                          String description,
                          @NotNull
                          @DecimalMin(value = "0.01")
                          BigDecimal price,
                          @NotNull
                          BillingCycle billingCycle) {
}
