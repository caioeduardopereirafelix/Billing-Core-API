package billing_core_api.controller;

import billing_core_api.domain.plan.Plan;
import billing_core_api.enums.BillingCycle;
import billing_core_api.repository.PlanRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * A user tops up their balance, subscribes (which charges the plan price), sees the
 * subscription under GET /subscription/me, cancels it, and never sees another user's.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SubscriptionFlowTest {

    @Autowired MockMvc mvc;
    @Autowired PlanRepository planRepository;

    private Long planId;

    @BeforeEach
    void seedPlan() {
        Plan plan = new Plan();
        plan.setName("Flow Plan " + System.nanoTime());
        plan.setDescription("test");
        plan.setPrice(new BigDecimal("19.90"));
        plan.setBillingCycle(BillingCycle.MONTHLY);
        plan.setActive(true);
        planId = planRepository.save(plan).getId();
    }

    private String tokenFor(String email) throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"U","email":"%s","password":"secret1"}
                        """.formatted(email))).andExpect(status().isCreated());
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secret1"}
                                """.formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private void deposit(String token, String amount) throws Exception {
        mvc.perform(post("/user/me/deposit").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":" + amount + "}"))
                .andExpect(status().isOk());
    }

    @Test
    void topUp_subscribe_seesItUnderMe_thenCancels() throws Exception {
        String alice = tokenFor("alice-" + System.nanoTime() + "@x.com");

        deposit(alice, "100.00");
        mvc.perform(get("/user/me").header("Authorization", alice))
                .andExpect(jsonPath("$.balance").value(100.0));

        // empty before subscribing
        mvc.perform(get("/subscription/me").header("Authorization", alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        String created = mvc.perform(post("/subscription").header("Authorization", alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":" + planId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVED"))
                .andReturn().getResponse().getContentAsString();
        int subId = JsonPath.read(created, "$.id");

        // the plan price was charged
        mvc.perform(get("/user/me").header("Authorization", alice))
                .andExpect(jsonPath("$.balance").value(80.1));

        mvc.perform(get("/subscription/me").header("Authorization", alice))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(subId))
                .andExpect(jsonPath("$[0].amount").exists())
                .andExpect(jsonPath("$[0].status").value("ACTIVED"))
                .andExpect(jsonPath("$[0].canceledAt").value(org.hamcrest.Matchers.nullValue()));

        mvc.perform(patch("/subscription/" + subId + "/cancel").header("Authorization", alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.canceledAt").isNotEmpty());

        mvc.perform(get("/subscription/me").header("Authorization", alice))
                .andExpect(jsonPath("$[0].canceledAt").isNotEmpty());
    }

    @Test
    void subscribe_withoutEnoughBalance_returns402() throws Exception {
        String broke = tokenFor("broke-" + System.nanoTime() + "@x.com");

        mvc.perform(post("/subscription").header("Authorization", broke)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":" + planId + "}"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value(402));

        // nothing was created
        mvc.perform(get("/subscription/me").header("Authorization", broke))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void me_isScopedToTheCaller() throws Exception {
        String alice = tokenFor("alice2-" + System.nanoTime() + "@x.com");
        String bob = tokenFor("bob-" + System.nanoTime() + "@x.com");

        deposit(alice, "50.00");
        mvc.perform(post("/subscription").header("Authorization", alice)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planId\":" + planId + "}")).andExpect(status().isCreated());

        mvc.perform(get("/subscription/me").header("Authorization", bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void me_requiresAuthentication() throws Exception {
        mvc.perform(get("/subscription/me")).andExpect(status().isUnauthorized());
    }
}
