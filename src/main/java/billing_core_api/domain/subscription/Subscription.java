package billing_core_api.domain.subscription;

import billing_core_api.config.AuditingClass;
import billing_core_api.domain.plan.Plan;
import billing_core_api.enums.SubscriptionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Subscription extends AuditingClass {

    public void cancel() {

        if (this.status == SubscriptionStatus.CANCELED) {
            throw new IllegalStateException("Assinatura já cancelada.");
        }

        this.status = SubscriptionStatus.CANCELED;
        this.endDate = LocalDateTime.now();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    @Email
    private String customerEmail;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;
}
