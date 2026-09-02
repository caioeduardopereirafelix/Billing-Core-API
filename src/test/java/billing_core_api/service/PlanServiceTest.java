package billing_core_api.service;

import billing_core_api.domain.plan.Plan;
import billing_core_api.dto.plan.PlanRequest;
import billing_core_api.dto.plan.UpdatePlanRequest;
import billing_core_api.enums.BillingCycle;
import billing_core_api.exception.BusinessRuleException;
import billing_core_api.exception.PlanAlreadyExists;
import billing_core_api.exception.PlanNotFound;
import billing_core_api.repository.PlanRepository;
import billing_core_api.service.PlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock PlanRepository repository;
    @InjectMocks
    PlanService service;

    private Plan plan;
    private PlanRequest request;

    @BeforeEach
    void setUp() {
        plan = new Plan();
        plan.setId(1L);
        plan.setName("Premium");
        plan.setDescription("Premium plan");
        plan.setPrice(new BigDecimal("99.90"));
        plan.setBillingCycle(BillingCycle.MONTHLY);
        plan.setActive(true);

        request = new PlanRequest(
                "Premium",
                "Premium plan",
                new BigDecimal("99.90"),
                BillingCycle.MONTHLY
        );
    }

    @Test
    void shouldCreatePlanSuccessfully() {
        when(repository.existsByName("Premium")).thenReturn(false);
        when(repository.save(any(Plan.class))).thenAnswer(inv -> {
            Plan saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Plan result = service.createPlan(request);

        assertNotNull(result);
        assertEquals("Premium", result.getName());
        assertEquals(new BigDecimal("99.90"), result.getPrice());
        assertEquals(BillingCycle.MONTHLY, result.getBillingCycle());
        assertTrue(result.getActive());

        verify(repository).existsByName("Premium");
        verify(repository).save(any(Plan.class));
    }

    @Test
    void shouldThrowWhenCreatingDuplicatedPlan() {
        when(repository.existsByName("Premium")).thenReturn(true);

        assertThrows(PlanAlreadyExists.class, () -> service.createPlan(request));

        verify(repository, never()).save(any());
    }

    @Test
    void shouldFindPlanById() {
        when(repository.findById(1L)).thenReturn(Optional.of(plan));

        Plan result = service.findById(1L);

        assertSame(plan, result);
        verify(repository).findById(1L);
    }

    @Test
    void shouldThrowWhenPlanDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PlanNotFound.class, () -> service.findById(99L));
    }

    @Test
    void shouldListAllPlans() {
        when(repository.findAll()).thenReturn(List.of(plan));

        List<Plan> result = service.listAll();

        assertEquals(1, result.size());
        assertSame(plan, result.get(0));
        verify(repository).findAll();
    }

    @Test
    void shouldDisableActivePlan() {
        when(repository.findById(1L)).thenReturn(Optional.of(plan));
        when(repository.save(plan)).thenReturn(plan);

        Plan result = service.disabledPlan(1L);

        assertFalse(result.getActive());
        verify(repository).save(plan);
    }

    @Test
    void shouldThrowWhenDisablingAlreadyDisabledPlan() {
        plan.setActive(false);
        when(repository.findById(1L)).thenReturn(Optional.of(plan));

        assertThrows(BusinessRuleException.class, () -> service.disabledPlan(1L));

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowWhenDisablingNonExistingPlan() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PlanNotFound.class, () -> service.disabledPlan(99L));
    }

    @Test
    void shouldUpdatePlanSuccessfully() {
        UpdatePlanRequest update = new UpdatePlanRequest(
                "Enterprise",
                new BigDecimal("199.90"),
                "Enterprise plan",
                BillingCycle.YEARLY
        );

        when(repository.findById(1L)).thenReturn(Optional.of(plan));
        when(repository.save(plan)).thenReturn(plan);

        Plan result = service.putPlan(1L, update);

        assertEquals("Enterprise", result.getName());
        assertEquals(new BigDecimal("199.90"), result.getPrice());
        assertEquals("Enterprise plan", result.getDescription());
        assertEquals(BillingCycle.YEARLY, result.getBillingCycle());
        assertTrue(result.getActive());

        verify(repository).save(plan);
    }

    @Test
    void shouldThrowWhenUpdatingNonExistingPlan() {
        UpdatePlanRequest update = new UpdatePlanRequest(
                "Enterprise",
                new BigDecimal("199.90"),
                "Enterprise plan",
                BillingCycle.YEARLY
        );

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PlanNotFound.class, () -> service.putPlan(99L, update));

        verify(repository, never()).save(any());
    }
}