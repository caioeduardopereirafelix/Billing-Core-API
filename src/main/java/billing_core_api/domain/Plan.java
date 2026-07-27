package billing_core_api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
public class Plan {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column
    private String namePlan;

    @Column
    private BigDecimal price;

    @Column
    private String descriptionPlan;

    @Column
    BillingCycle billingCycle;

    @Column
    private boolean activate;


}
