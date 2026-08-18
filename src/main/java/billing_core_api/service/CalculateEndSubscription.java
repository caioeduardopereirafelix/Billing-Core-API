package billing_core_api.service;

import billing_core_api.domain.subscription.BillingCycle;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class CalculateEndSubscription {

    public LocalDateTime calculateEndDate(LocalDate startDate, BillingCycle billingCycle){

        return switch (billingCycle){

            case MONTHLY ->
                startDate.plusMonths(1)
                        .atTime(23,59, 59);

            case QUARTERLY -> startDate.plusMonths(3)
                    .atTime(23, 59, 59);

            case YEARLY ->
                startDate.plusYears(1)
                        .atTime(23,59, 59);
        };
    }
}
