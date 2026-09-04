package billing_core_api.dto.user;

import java.math.BigDecimal;

public record ResponseUserDTO(String id,
                              String name,
                              String email,
                              BigDecimal balance) {
}
