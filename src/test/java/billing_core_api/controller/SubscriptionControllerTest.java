package billing_core_api.controller;

import billing_core_api.config.JwtAuthenticationFilter;
import billing_core_api.config.SecurityConfig;
import billing_core_api.config.SecurityUtils;
import billing_core_api.config.TokenProvider;
import billing_core_api.domain.plan.Plan;
import billing_core_api.domain.subscription.Subscription;
import billing_core_api.domain.user.RolesUser;
import billing_core_api.domain.user.User;
import billing_core_api.enums.BillingCycle;
import billing_core_api.enums.RoleTypeEnum;
import billing_core_api.enums.SubscriptionStatus;
import billing_core_api.service.SubscriptionService;
import billing_core_api.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SecurityUtils.class})
class SubscriptionControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SubscriptionService subscriptionService;

    @MockitoBean
    TokenProvider tokenProvider;

    @MockitoBean
    UserDetailsServiceImpl userDetailsService;

    private User authenticatedUser(String email, RoleTypeEnum role) {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email(email)
                .password("hashed")
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

    private Subscription subscription(Long id, User owner) {
        Plan plan = new Plan();
        plan.setId(1L);
        plan.setName("Basic");
        plan.setPrice(new BigDecimal("19.90"));
        plan.setBillingCycle(BillingCycle.MONTHLY);
        plan.setActive(true);

        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setCustomerName(owner.getName());
        subscription.setCustomerEmail(owner.getEmail());
        subscription.setUser(owner);
        subscription.setPlan(plan);
        subscription.setAmount(plan.getPrice());
        subscription.setStartDate(LocalDate.now());
        subscription.setStatus(SubscriptionStatus.ACTIVED);
        return subscription;
    }

    @Test
    void createSubscription_asUser_returns201() throws Exception {
        User alice = authenticatedUser("alice@x.com", RoleTypeEnum.ROLE_USER);
        String token = tokenFor(alice);
        when(subscriptionService.createSubscription(any(), any())).thenReturn(subscription(1L, alice));

        mvc.perform(post("/subscription").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVED"));
    }

    @Test
    void createSubscription_withoutAuthentication_isUnauthorized() throws Exception {
        mvc.perform(post("/subscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listMine_returnsOnlyTheCallersSubscriptions() throws Exception {
        User alice = authenticatedUser("alice2@x.com", RoleTypeEnum.ROLE_USER);
        String token = tokenFor(alice);
        when(subscriptionService.listByUser(alice.getId())).thenReturn(List.of(subscription(1L, alice)));

        mvc.perform(get("/subscription/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerEmail").value("alice2@x.com"));
    }

    @Test
    void findById_asOwner_returns200() throws Exception {
        User alice = authenticatedUser("alice3@x.com", RoleTypeEnum.ROLE_USER);
        String token = tokenFor(alice);
        when(subscriptionService.buscarPorId(1L)).thenReturn(subscription(1L, alice));

        mvc.perform(get("/subscription/1").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void findById_asAnotherUser_isForbidden() throws Exception {
        User owner = authenticatedUser("owner@x.com", RoleTypeEnum.ROLE_USER);
        User intruder = authenticatedUser("intruder@x.com", RoleTypeEnum.ROLE_USER);
        String token = tokenFor(intruder);
        when(subscriptionService.buscarPorId(1L)).thenReturn(subscription(1L, owner));

        mvc.perform(get("/subscription/1").header("Authorization", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void findById_asAdmin_canSeeAnyonesSubscription() throws Exception {
        User owner = authenticatedUser("owner2@x.com", RoleTypeEnum.ROLE_USER);
        User admin = authenticatedUser("admin@x.com", RoleTypeEnum.ROLE_ADMIN);
        String token = tokenFor(admin);
        when(subscriptionService.buscarPorId(1L)).thenReturn(subscription(1L, owner));

        mvc.perform(get("/subscription/1").header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    void listAll_asAdmin_returns200() throws Exception {
        User admin = authenticatedUser("admin2@x.com", RoleTypeEnum.ROLE_ADMIN);
        String token = tokenFor(admin);
        when(subscriptionService.listAll()).thenReturn(List.of(subscription(1L, admin)));

        mvc.perform(get("/subscription").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listAll_asRegularUser_isForbidden() throws Exception {
        User user = authenticatedUser("user@x.com", RoleTypeEnum.ROLE_USER);
        String token = tokenFor(user);

        mvc.perform(get("/subscription").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelSubscription_asOwner_returns200() throws Exception {
        User alice = authenticatedUser("alice4@x.com", RoleTypeEnum.ROLE_USER);
        String token = tokenFor(alice);
        when(subscriptionService.buscarPorId(1L)).thenReturn(subscription(1L, alice));
        Subscription canceled = subscription(1L, alice);
        canceled.setStatus(SubscriptionStatus.CANCELED);
        when(subscriptionService.cancelSubscription(1L)).thenReturn(canceled);

        mvc.perform(patch("/subscription/1/cancel").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void cancelSubscription_asAnotherUser_isForbidden() throws Exception {
        User owner = authenticatedUser("owner3@x.com", RoleTypeEnum.ROLE_USER);
        User intruder = authenticatedUser("intruder2@x.com", RoleTypeEnum.ROLE_USER);
        String token = tokenFor(intruder);
        when(subscriptionService.buscarPorId(1L)).thenReturn(subscription(1L, owner));

        mvc.perform(patch("/subscription/1/cancel").header("Authorization", token))
                .andExpect(status().isForbidden());
    }
}
