package billing_core_api.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void planNotFound_maps_to_404() throws Exception {
        mvc.perform(get("/t/plan-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("no plan"));
    }

    @Test
    void subscriptionNotFound_maps_to_404() throws Exception {
        mvc.perform(get("/t/subscription-not-found"))
                .andExpect(status().isNotFound());
    }

    @Test
    void planAlreadyExists_maps_to_409() throws Exception {
        mvc.perform(get("/t/plan-already-exists"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void illegalArgument_maps_to_400() throws Exception {
        mvc.perform(get("/t/illegal-argument"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidCredentials_maps_to_401() throws Exception {
        mvc.perform(get("/t/invalid-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void accessDenied_maps_to_403() throws Exception {
        mvc.perform(get("/t/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void businessRule_maps_to_409() throws Exception {
        mvc.perform(get("/t/business-rule"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void insufficientBalance_maps_to_402() throws Exception {
        mvc.perform(get("/t/insufficient-balance"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value(402));
    }

    @Test
    void fieldIsBlank_maps_to_400() throws Exception {
        mvc.perform(get("/t/field-is-blank"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void amqpDown_maps_to_503() throws Exception {
        mvc.perform(get("/t/amqp-down"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void pathVariableTypeMismatch_maps_to_400() throws Exception {
        mvc.perform(get("/t/number/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldsError[0].field").value("id"));
    }

    @Test
    void unreadableBody_maps_to_400() throws Exception {
        mvc.perform(post("/t/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed or missing request body"));
    }

    @RestController
    @RequestMapping("/t")
    static class ThrowingController {

        @GetMapping("/plan-not-found")
        void planNotFound() {
            throw new PlanNotFound("no plan");
        }

        @GetMapping("/subscription-not-found")
        void subscriptionNotFound() {
            throw new SubscriptionNotFoundException("no subscription");
        }

        @GetMapping("/plan-already-exists")
        void planAlreadyExists() {
            throw new PlanAlreadyExists("dup plan");
        }

        @GetMapping("/illegal-argument")
        void illegalArgument() {
            throw new IllegalArgumentException("bad arg");
        }

        @GetMapping("/invalid-credentials")
        void invalidCredentials() {
            throw new InvalidCredentialsException("nope");
        }

        @GetMapping("/access-denied")
        void accessDenied() {
            throw new AccessDeniedException("forbidden");
        }

        @GetMapping("/business-rule")
        void businessRule() {
            throw new BusinessRuleException("conflict");
        }

        @GetMapping("/insufficient-balance")
        void insufficientBalance() {
            throw new InsufficientBalanceException("not enough credit");
        }

        @GetMapping("/field-is-blank")
        void fieldIsBlank() {
            throw new FieldIsBlank("blank");
        }

        @GetMapping("/amqp-down")
        void amqpDown() {
            throw new AmqpException("broker down");
        }

        @GetMapping("/number/{id}")
        void number(@PathVariable Long id) {
            // reached only with a valid Long; type mismatch is handled before this runs
        }

        @PostMapping("/body")
        void body(@RequestBody Payload payload) {
            // reached only with a parseable body
        }

        record Payload(String name) {
        }
    }
}
