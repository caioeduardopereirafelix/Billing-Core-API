package billing_core_api.dto;

import billing_core_api.dto.auth.LoginRequestDTO;
import billing_core_api.dto.plan.PlanRequest;
import billing_core_api.dto.plan.UpdatePlanRequest;
import billing_core_api.dto.subscription.SubscriptionRequest;
import billing_core_api.dto.user.CreateUserDTO;
import billing_core_api.enums.BillingCycle;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DTOValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void beforeAll() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void afterAll() {
        factory.close();
    }

    @Test
    void shouldAcceptValidCreateUserDTO() {
        var dto = new CreateUserDTO("Caio", "caio@email.com", "123456");
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void shouldRejectInvalidCreateUserDTO() {
        var dto = new CreateUserDTO("", "invalid", "123");
        var violations = validator.validate(dto);

        assertTrue(violations.size() >= 3);
    }

    @Test
    void shouldRejectNullName() {
        var dto = new CreateUserDTO(null, "caio@email.com", "123456");
        assertTrue(validator.validate(dto).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void shouldRejectShortPassword() {
        var dto = new CreateUserDTO("Caio", "caio@email.com", "123");
        assertTrue(validator.validate(dto).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void shouldAcceptValidLoginDTO() {
        var dto = new LoginRequestDTO("caio@email.com", "12345");
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void shouldRejectInvalidLoginDTO() {
        var dto = new LoginRequestDTO("", "123");
        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void shouldAcceptValidPlanRequest() {
        var dto = new PlanRequest(
                "Premium",
                "Premium plan",
                new BigDecimal("0.01"),
                BillingCycle.MONTHLY
        );
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void shouldRejectInvalidPlanRequest() {
        var dto = new PlanRequest(
                "",
                "",
                BigDecimal.ZERO,
                null
        );
        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void shouldRejectNegativePlanPrice() {
        var dto = new PlanRequest(
                "Premium",
                "desc",
                new BigDecimal("-1"),
                BillingCycle.MONTHLY
        );
        assertTrue(validator.validate(dto).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("price")));
    }

    @Test
    void shouldAcceptValidUpdatePlanRequest() {
        var dto = new UpdatePlanRequest(
                "Enterprise",
                new BigDecimal("100"),
                "desc",
                BillingCycle.YEARLY
        );
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void shouldRejectInvalidUpdatePlanRequest() {
        var dto = new UpdatePlanRequest(
                "",
                BigDecimal.ZERO,
                "",
                null
        );
        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void shouldAcceptValidSubscriptionRequest() {
        var dto = new SubscriptionRequest(
                "caio@email.com",
                "Caio",
                1L
        );
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void shouldRejectInvalidSubscriptionRequest() {
        var dto = new SubscriptionRequest("", "", 0L);
        assertFalse(validator.validate(dto).isEmpty());
    }
}
