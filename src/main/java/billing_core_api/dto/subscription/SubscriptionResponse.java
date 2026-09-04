package billing_core_api.dto.subscription;

import billing_core_api.domain.subscription.Subscription;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SubscriptionResponse(Long id,
                                   String customerEmail,
                                   String customerName,
                                   String planName,
                                   BigDecimal amount,
                                   LocalDate startDate,
                                   LocalDateTime endDate,
                                   LocalDateTime canceledAt,
                                   String status) {

    public static SubscriptionResponse from(Subscription s) {
        return new SubscriptionResponse(
                s.getId(),
                s.getCustomerEmail(),
                s.getCustomerName(),
                s.getPlan().getName(),
                s.getAmount(),
                s.getStartDate(),
                s.getEndDate(),
                s.getCanceledAt(),
                s.getStatus().toString()
        );
    }
}
