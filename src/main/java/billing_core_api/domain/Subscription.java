package billing_core_api.domain;

import billing_core_api.auditing.AuditingClass;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
public class Subscription extends AuditingClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String customerEmail;

    @Column
    private String customerName;

    @Column
    private Plan planName;

    @Column
    private BigDecimal amount;

    @Column
    private LocalDate startDate;

    @Column
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;
}
