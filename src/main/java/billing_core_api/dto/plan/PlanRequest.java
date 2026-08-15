package billing_core_api.dto.plan;

import billing_core_api.domain.subscription.BillingCycle;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;

public record PlanRequest (@NotBlank
                          String namePlan,
                          @NotBlank
                          String description,
                          @NotNull
                          @DecimalMin(value = "0.01")
                          BigDecimal price,
                          @NotNull
                          BillingCycle billingCycle) implements Serializable {
}
