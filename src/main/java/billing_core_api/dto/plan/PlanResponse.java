package billing_core_api.dto.plan;

import java.math.BigDecimal;

public record PlanResponse(Long id,
                           String name,
                           BigDecimal price,
                           boolean active) {
}
