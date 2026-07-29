package billing_core_api.repository;

import billing_core_api.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByActiveTrue();

    boolean existsByName(String name);
}
