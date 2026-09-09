package billing_core_api.dto.plan;

import billing_core_api.domain.plan.Plan;
import billing_core_api.enums.BillingCycle;

import java.math.BigDecimal;

public record PlanResponse(Long id,
                           String name,
                           String description,
                           BigDecimal price,
                           BillingCycle billingCycle,
                           boolean active) {

    public static PlanResponse from(Plan plan) {
        return new PlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getDescription(),
                plan.getPrice(),
                plan.getBillingCycle(),
                plan.getActive()
        );
    }
}
