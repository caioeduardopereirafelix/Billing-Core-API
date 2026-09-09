package billing_core_api.dto;

import billing_core_api.domain.plan.Plan;
import billing_core_api.dto.plan.PlanResponse;
import billing_core_api.enums.BillingCycle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PlanResponseTest {

    @Test
    void fromCopiesEveryFieldOfThePlan() {
        Plan plan = new Plan();
        plan.setId(7L);
        plan.setName("Pro");
        plan.setDescription("Tudo do Basic, mais relatórios");
        plan.setPrice(new BigDecimal("99.90"));
        plan.setBillingCycle(BillingCycle.YEARLY);
        plan.setActive(true);

        PlanResponse response = PlanResponse.from(plan);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.name()).isEqualTo("Pro");
        assertThat(response.description()).isEqualTo("Tudo do Basic, mais relatórios");
        assertThat(response.price()).isEqualByComparingTo("99.90");
        assertThat(response.billingCycle()).isEqualTo(BillingCycle.YEARLY);
        assertThat(response.active()).isTrue();
    }

    @Test
    void fromKeepsInactivePlansMarkedAsInactive() {
        Plan plan = new Plan();
        plan.setId(1L);
        plan.setName("Legacy");
        plan.setDescription("descontinuado");
        plan.setPrice(new BigDecimal("10.00"));
        plan.setBillingCycle(BillingCycle.MONTHLY);
        plan.setActive(false);

        assertThat(PlanResponse.from(plan).active()).isFalse();
    }
}
