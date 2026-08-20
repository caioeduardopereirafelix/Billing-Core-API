package billing_core_api.repository;

import billing_core_api.domain.user.RolesUser;
import billing_core_api.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RolesUser, Long> {
    Optional<RolesUser> findByName(String role);
}
