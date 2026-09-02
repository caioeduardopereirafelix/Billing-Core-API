package billing_core_api.config;

import billing_core_api.domain.user.RolesUser;
import billing_core_api.domain.user.User;
import billing_core_api.enums.RoleTypeEnum;
import billing_core_api.exception.InvalidCredentialsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    private final SecurityUtils securityUtils = new SecurityUtils();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private User authenticateAs(UUID id, RoleTypeEnum... roles) {
        User user = User.builder()
                .id(id)
                .email("user@example.com")
                .roles(List.of(roles).stream()
                        .map(r -> RolesUser.builder().name(r.name()).build())
                        .toList())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        return user;
    }

    @Test
    void getAuthenticatedUser_throwsWhenContextIsEmpty() {
        assertThrows(InvalidCredentialsException.class, securityUtils::getAuthenticatedUser);
    }

    @Test
    void getAuthenticatedUser_throwsWhenPrincipalIsNotOurUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymous", null, List.of()));

        assertThrows(InvalidCredentialsException.class, securityUtils::getAuthenticatedUser);
    }

    @Test
    void hasRole_matchesWithAndWithoutRolePrefix() {
        authenticateAs(UUID.randomUUID(), RoleTypeEnum.ROLE_USER);

        assertTrue(securityUtils.hasRole("USER"));
        assertTrue(securityUtils.hasRole("ROLE_USER"));
        assertFalse(securityUtils.hasRole("ADMIN"));
        assertFalse(securityUtils.isAdmin());
    }

    @Test
    void requireOwnerOrAdmin_allowsTheOwner() {
        UUID id = UUID.randomUUID();
        authenticateAs(id, RoleTypeEnum.ROLE_USER);

        assertDoesNotThrow(() -> securityUtils.requireOwnerOrAdmin(id));
    }

    @Test
    void requireOwnerOrAdmin_allowsAnAdminActingOnSomeoneElse() {
        authenticateAs(UUID.randomUUID(), RoleTypeEnum.ROLE_ADMIN);

        assertDoesNotThrow(() -> securityUtils.requireOwnerOrAdmin(UUID.randomUUID()));
    }

    @Test
    void requireOwnerOrAdmin_rejectsANonOwnerNonAdmin() {
        authenticateAs(UUID.randomUUID(), RoleTypeEnum.ROLE_USER);

        assertThrows(AccessDeniedException.class,
                () -> securityUtils.requireOwnerOrAdmin(UUID.randomUUID()));
    }
}
