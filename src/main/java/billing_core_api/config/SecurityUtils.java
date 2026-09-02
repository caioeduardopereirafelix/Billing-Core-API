package billing_core_api.config;

import billing_core_api.domain.user.User;
import billing_core_api.exception.InvalidCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtils {

    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new InvalidCredentialsException("No authenticated user in the security context");
        }
        return user;
    }

    public boolean hasRole(String role) {
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return getAuthenticatedUser().getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals(authority));
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }


    public void requireOwnerOrAdmin(UUID ownerId) {
        if (!getAuthenticatedUser().getId().equals(ownerId) && !isAdmin()) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }
}
