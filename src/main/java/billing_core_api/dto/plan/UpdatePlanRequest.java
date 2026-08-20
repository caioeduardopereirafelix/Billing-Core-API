package billing_core_api.dto.plan;

import billing_core_api.enums.BillingCycle;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdatePlanRequest(@NotBlank
                                String newNamePlan,
                                @NotNull
                                @DecimalMin(value = "0.01")
                                BigDecimal newPrice,
                                @NotBlank
                                String newDescription,
                                @NotNull
                                BillingCycle newBillingCycle
                                ) {
}
