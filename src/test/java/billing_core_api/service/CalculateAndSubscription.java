package billing_core_api.service;

import billing_core_api.enums.BillingCycle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CalculateEndSubscriptionTest {

    private final CalculateAndSubscription service = new CalculateAndSubscription();

    @Test
    void shouldCalculateMonthlyEndDate() {
        LocalDate start = LocalDate.of(2026, 8, 20);

        LocalDateTime result = service.calculateEndDate(start, BillingCycle.MONTHLY);

        assertEquals(LocalDateTime.of(2026, 9, 20, 23, 59, 59), result);
    }

    @Test
    void shouldCalculateQuarterlyEndDate() {
        LocalDate start = LocalDate.of(2026, 8, 20);

        LocalDateTime result = service.calculateEndDate(start, BillingCycle.QUARTERLY);

        assertEquals(LocalDateTime.of(2026, 11, 20, 23, 59, 59), result);
    }

    @Test
    void shouldCalculateYearlyEndDate() {
        LocalDate start = LocalDate.of(2026, 8, 20);

        LocalDateTime result = service.calculateEndDate(start, BillingCycle.YEARLY);

        assertEquals(LocalDateTime.of(2027, 8, 20, 23, 59, 59), result);
    }

    @Test
    void shouldHandleLeapDayWhenAddingOneYear() {
        LocalDate start = LocalDate.of(2024, 2, 29);

        LocalDateTime result = service.calculateEndDate(start, BillingCycle.YEARLY);

        assertEquals(LocalDateTime.of(2025, 2, 28, 23, 59, 59), result);
    }
}
