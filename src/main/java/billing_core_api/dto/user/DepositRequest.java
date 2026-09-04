package billing_core_api.dto.user;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DepositRequest(@NotNull
                             @Positive
                             @Digits(integer = 10, fraction = 2)
                             BigDecimal amount) {
}
