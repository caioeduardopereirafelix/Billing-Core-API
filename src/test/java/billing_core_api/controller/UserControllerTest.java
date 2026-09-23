package billing_core_api.controller;

import billing_core_api.config.JwtAuthenticationFilter;
import billing_core_api.config.SecurityConfig;
import billing_core_api.config.SecurityUtils;
import billing_core_api.config.TokenProvider;
import billing_core_api.domain.mapper.UserMapperImpl;
import billing_core_api.domain.user.RolesUser;
import billing_core_api.domain.user.User;
import billing_core_api.enums.RoleTypeEnum;
import billing_core_api.exception.UserNotFound;
import billing_core_api.service.UserDetailsServiceImpl;
import billing_core_api.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SecurityUtils.class, UserMapperImpl.class})
class UserControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserService userService;

    @MockitoBean
    TokenProvider tokenProvider;

    @MockitoBean
    UserDetailsServiceImpl userDetailsService;

    private User authenticatedUser(String email, RoleTypeEnum role, BigDecimal balance) {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email(email)
                .password("hashed")
                .balance(balance)
                .roles(List.of(RolesUser.builder().id(1).name(role.name()).build()))
                .build();
    }

    private String tokenFor(User user) {
        String tokenValue = user.getEmail() + "-token";
        when(tokenProvider.isTokenValid(tokenValue)).thenReturn(true);
        when(tokenProvider.getUsername(tokenValue)).thenReturn(user.getEmail());
        when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(user);
        return "Bearer " + tokenValue;
    }

    @Test
    void me_returnsAuthenticatedUsersProfile() throws Exception {
        User alice = authenticatedUser("alice@x.com", RoleTypeEnum.ROLE_USER, new BigDecimal("50.00"));
        String token = tokenFor(alice);
        when(userService.getById(alice.getId())).thenReturn(alice);

        mvc.perform(get("/user/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@x.com"))
                .andExpect(jsonPath("$.balance").value(50.00));
    }

    @Test
    void me_withoutAuthentication_isUnauthorized() throws Exception {
        mvc.perform(get("/user/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deposit_withValidAmount_returns200() throws Exception {
        User alice = authenticatedUser("alice2@x.com", RoleTypeEnum.ROLE_USER, new BigDecimal("100.00"));
        String token = tokenFor(alice);
        User afterDeposit = authenticatedUser("alice2@x.com", RoleTypeEnum.ROLE_USER, new BigDecimal("150.00"));
        when(userService.deposit(eq(alice.getId()), eq(new BigDecimal("50.00")))).thenReturn(afterDeposit);

        mvc.perform(post("/user/me/deposit").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    void deposit_withNegativeAmount_returns400() throws Exception {
        User alice = authenticatedUser("alice3@x.com", RoleTypeEnum.ROLE_USER, BigDecimal.ZERO);
        String token = tokenFor(alice);

        mvc.perform(post("/user/me/deposit").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":-10.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDetails_asOwner_returns200() throws Exception {
        User alice = authenticatedUser("alice4@x.com", RoleTypeEnum.ROLE_USER, BigDecimal.ZERO);
        String token = tokenFor(alice);
        when(userService.findById(alice.getId())).thenReturn(Optional.of(alice));

        mvc.perform(get("/user/" + alice.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice4@x.com"));
    }

    @Test
    void getDetails_asAnotherUser_isForbidden() throws Exception {
        User alice = authenticatedUser("alice5@x.com", RoleTypeEnum.ROLE_USER, BigDecimal.ZERO);
        User bob = authenticatedUser("bob@x.com", RoleTypeEnum.ROLE_USER, BigDecimal.ZERO);
        String token = tokenFor(bob);

        mvc.perform(get("/user/" + alice.getId()).header("Authorization", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getDetails_whenUserMissing_returns404() throws Exception {
        User admin = authenticatedUser("admin@x.com", RoleTypeEnum.ROLE_ADMIN, BigDecimal.ZERO);
        String token = tokenFor(admin);
        UUID missingId = UUID.randomUUID();
        when(userService.findById(missingId)).thenReturn(Optional.empty());

        mvc.perform(get("/user/" + missingId).header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDetails_withMalformedId_returns400() throws Exception {
        User admin = authenticatedUser("admin2@x.com", RoleTypeEnum.ROLE_ADMIN, BigDecimal.ZERO);
        String token = tokenFor(admin);

        mvc.perform(get("/user/not-a-uuid").header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_asOwner_returns202() throws Exception {
        User alice = authenticatedUser("alice6@x.com", RoleTypeEnum.ROLE_USER, BigDecimal.ZERO);
        String token = tokenFor(alice);
        when(userService.findById(alice.getId())).thenReturn(Optional.of(alice));

        mvc.perform(delete("/user/" + alice.getId()).header("Authorization", token))
                .andExpect(status().isAccepted());
    }

    @Test
    void deleteUser_asAnotherUser_isForbidden() throws Exception {
        User alice = authenticatedUser("alice7@x.com", RoleTypeEnum.ROLE_USER, BigDecimal.ZERO);
        User bob = authenticatedUser("bob2@x.com", RoleTypeEnum.ROLE_USER, BigDecimal.ZERO);
        String token = tokenFor(bob);

        mvc.perform(delete("/user/" + alice.getId()).header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_asOwner_returns200() throws Exception {
        User alice = authenticatedUser("alice8@x.com", RoleTypeEnum.ROLE_USER, BigDecimal.ZERO);
        String token = tokenFor(alice);
        User updated = authenticatedUser("new-email@x.com", RoleTypeEnum.ROLE_USER, BigDecimal.ZERO);
        when(userService.updateUser(eq(alice.getId()), any())).thenReturn(updated);

        mvc.perform(put("/user/" + alice.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New Name","email":"new-email@x.com","password":null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new-email@x.com"));
    }

    @Test
    void updateUser_asAnotherUser_isForbidden() throws Exception {
        User alice = authenticatedUser("alice9@x.com", RoleTypeEnum.ROLE_USER, BigDecimal.ZERO);
        User bob = authenticatedUser("bob3@x.com", RoleTypeEnum.ROLE_USER, BigDecimal.ZERO);
        String token = tokenFor(bob);

        mvc.perform(put("/user/" + alice.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Hacked","email":"hacked@x.com","password":null}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_whenUserMissing_returns404() throws Exception {
        User admin = authenticatedUser("admin3@x.com", RoleTypeEnum.ROLE_ADMIN, BigDecimal.ZERO);
        String token = tokenFor(admin);
        UUID missingId = UUID.randomUUID();
        when(userService.updateUser(eq(missingId), any()))
                .thenThrow(new UserNotFound("User " + missingId + " not found"));

        mvc.perform(put("/user/" + missingId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ghost","email":"ghost@x.com","password":null}
                                """))
                .andExpect(status().isNotFound());
    }
}
