package billing_core_api.repository;

import billing_core_api.domain.subscription.Subscription;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @EntityGraph(attributePaths = {"plan", "user"})
    List<Subscription> findByUser_IdOrderByStartDateDesc(UUID userId);
}
