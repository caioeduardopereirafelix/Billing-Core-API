package billing_core_api.controller;

import billing_core_api.config.JwtAuthenticationFilter;
import billing_core_api.config.SecurityConfig;
import billing_core_api.config.TokenProvider;
import billing_core_api.domain.plan.Plan;
import billing_core_api.domain.user.RolesUser;
import billing_core_api.domain.user.User;
import billing_core_api.enums.BillingCycle;
import billing_core_api.enums.RoleTypeEnum;
import billing_core_api.exception.BusinessRuleException;
import billing_core_api.exception.PlanNotFound;
import billing_core_api.service.PlanService;
import billing_core_api.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlanController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PlanControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PlanService planService;

    @MockitoBean
    TokenProvider tokenProvider;

    @MockitoBean
    UserDetailsServiceImpl userDetailsService;

    private String tokenFor(String email, RoleTypeEnum role) {
        User user = User.builder()
                .name("Test User")
                .email(email)
                .password("hashed")
                .roles(List.of(RolesUser.builder().id(1).name(role.name()).build()))
                .build();

        when(tokenProvider.isTokenValid(email + "-token")).thenReturn(true);
        when(tokenProvider.getUsername(email + "-token")).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(user);

        return "Bearer " + email + "-token";
    }

    private Plan plan(Long id, String name, boolean active) {
        Plan plan = new Plan();
        plan.setId(id);
        plan.setName(name);
        plan.setDescription("desc");
        plan.setPrice(new BigDecimal("19.90"));
        plan.setBillingCycle(BillingCycle.MONTHLY);
        plan.setActive(active);
        return plan;
    }

    @Test
    void createPlan_asAdmin_returns201() throws Exception {
        String admin = tokenFor("admin@x.com", RoleTypeEnum.ROLE_ADMIN);
        when(planService.createPlan(any())).thenReturn(plan(1L, "Pro", true));

        mvc.perform(post("/plan").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"namePlan":"Pro","description":"desc","price":19.90,"billingCycle":"MONTHLY"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Pro"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createPlan_asRegularUser_isForbidden() throws Exception {
        String user = tokenFor("user@x.com", RoleTypeEnum.ROLE_USER);

        mvc.perform(post("/plan").header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"namePlan":"Pro","description":"desc","price":19.90,"billingCycle":"MONTHLY"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createPlan_withoutAuthentication_isUnauthorized() throws Exception {
        mvc.perform(post("/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"namePlan":"Pro","description":"desc","price":19.90,"billingCycle":"MONTHLY"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPlan_withInvalidBody_returns400() throws Exception {
        String admin = tokenFor("admin2@x.com", RoleTypeEnum.ROLE_ADMIN);

        mvc.perform(post("/plan").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"namePlan":"","description":"desc","price":-1,"billingCycle":"MONTHLY"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findById_asUser_returnsPlan() throws Exception {
        String user = tokenFor("user2@x.com", RoleTypeEnum.ROLE_USER);
        when(planService.findById(1L)).thenReturn(plan(1L, "Basic", true));

        mvc.perform(get("/plan/1").header("Authorization", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Basic"));
    }

    @Test
    void findById_whenPlanMissing_returns404() throws Exception {
        String user = tokenFor("user3@x.com", RoleTypeEnum.ROLE_USER);
        when(planService.findById(99L)).thenThrow(new PlanNotFound("Plan not exist, try again later"));

        mvc.perform(get("/plan/99").header("Authorization", user))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listAll_asUser_returnsPlans() throws Exception {
        String user = tokenFor("user4@x.com", RoleTypeEnum.ROLE_USER);
        when(planService.listAll()).thenReturn(List.of(plan(1L, "Basic", true), plan(2L, "Pro", true)));

        mvc.perform(get("/plan").header("Authorization", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void cancelPlan_asAdmin_returns200() throws Exception {
        String admin = tokenFor("admin3@x.com", RoleTypeEnum.ROLE_ADMIN);
        when(planService.disabledPlan(1L)).thenReturn(plan(1L, "Basic", false));

        mvc.perform(patch("/plan/1/cancel").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void cancelPlan_asRegularUser_isForbidden() throws Exception {
        String user = tokenFor("user5@x.com", RoleTypeEnum.ROLE_USER);

        mvc.perform(patch("/plan/1/cancel").header("Authorization", user))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelPlan_whenAlreadyInactive_returns409() throws Exception {
        String admin = tokenFor("admin4@x.com", RoleTypeEnum.ROLE_ADMIN);
        when(planService.disabledPlan(1L)).thenThrow(new BusinessRuleException("Plan is already inactive"));

        mvc.perform(patch("/plan/1/cancel").header("Authorization", admin))
                .andExpect(status().isConflict());
    }

    @Test
    void changePrice_asAdmin_returns200() throws Exception {
        String admin = tokenFor("admin5@x.com", RoleTypeEnum.ROLE_ADMIN);
        when(planService.putPlan(eq(1L), any())).thenReturn(plan(1L, "Pro Plus", true));

        mvc.perform(put("/plan/1").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newNamePlan":"Pro Plus","newPrice":29.90,"newDescription":"desc","newBillingCycle":"MONTHLY"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pro Plus"));
    }

    @Test
    void changePrice_withInvalidBody_returns400() throws Exception {
        String admin = tokenFor("admin6@x.com", RoleTypeEnum.ROLE_ADMIN);

        mvc.perform(put("/plan/1").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newNamePlan":"","newPrice":0,"newDescription":"","newBillingCycle":null}
                                """))
                .andExpect(status().isBadRequest());
    }
}
