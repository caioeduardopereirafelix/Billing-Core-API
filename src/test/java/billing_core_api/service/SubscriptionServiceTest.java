package billing_core_api.service;

import billing_core_api.domain.plan.Plan;
import billing_core_api.domain.subscription.Subscription;
import billing_core_api.dto.subscription.SubscriptionRequest;
import billing_core_api.enums.BillingCycle;
import billing_core_api.enums.SubscriptionStatus;
import billing_core_api.exception.PlanNotFound;
import billing_core_api.exception.SubscriptionNotFoundException;
import billing_core_api.messaging.SubscriptionEventPublisher;
import billing_core_api.repository.PlanRepository;
import billing_core_api.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock SubscriptionRepository repository;
    @Mock PlanRepository planRepository;
    @Mock SubscriptionEventPublisher eventPublisher;
    @Mock
    CalculateAndSubscription calculateEndSubscription;

    @InjectMocks SubscriptionService service;

    private Plan plan;
    private SubscriptionRequest request;

    @BeforeEach
    void setUp() {
        plan = new Plan();
        plan.setId(1L);
        plan.setName("Premium");
        plan.setPrice(new BigDecimal("99.90"));
        plan.setBillingCycle(BillingCycle.MONTHLY);
        plan.setActive(true);

        request = new SubscriptionRequest(
                "customer@email.com",
                "Customer",
                1L
        );
    }

    @Test
    void shouldCreateSubscriptionSuccessfully() {
        LocalDate today = LocalDate.now();
        LocalDateTime end = today.plusMonths(1).atTime(23, 59, 59);

        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(calculateEndSubscription.calculateEndDate(today, BillingCycle.MONTHLY)).thenReturn(end);
        when(repository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        Subscription result = service.createSubscription(request);

        assertEquals(10L, result.getId());
        assertEquals("Customer", result.getCustomerName());
        assertEquals("customer@email.com", result.getCustomerEmail());
        assertSame(plan, result.getPlan());
        assertEquals(new BigDecimal("99.90"), result.getAmount());
        assertEquals(today, result.getStartDate());
        assertEquals(end, result.getEndDate());
        assertEquals(SubscriptionStatus.ACTIVED, result.getStatus());
        assertNotNull(result.getCreatedDate());

        verify(repository).save(any(Subscription.class));
        verify(eventPublisher).publishSubscriptionCreated(any());
    }

    @Test
    void shouldThrowWhenPlanDoesNotExist() {
        when(planRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(PlanNotFound.class, () -> service.createSubscription(request));

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishSubscriptionCreated(any());
    }

    @Test
    void shouldPropagateEndDateCalculatorFailureAndNotPublishEvent() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(calculateEndSubscription.calculateEndDate(any(), eq(BillingCycle.MONTHLY)))
                .thenThrow(new IllegalStateException("calculation failure"));

        assertThrows(IllegalStateException.class, () -> service.createSubscription(request));

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishSubscriptionCreated(any());
    }

    @Test
    void shouldFindSubscriptionById() {
        Subscription subscription = new Subscription();
        subscription.setId(10L);

        when(repository.findById(10L)).thenReturn(Optional.of(subscription));

        assertSame(subscription, service.buscarPorId(10L));
    }

    @Test
    void shouldThrowWhenSubscriptionDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                SubscriptionNotFoundException.class,
                () -> service.buscarPorId(99L)
        );
    }

    @Test
    void shouldListAllSubscriptions() {
        Subscription subscription = new Subscription();
        subscription.setId(10L);

        when(repository.findAll()).thenReturn(List.of(subscription));

        List<Subscription> result = service.listAll();

        assertEquals(1, result.size());
        assertSame(subscription, result.get(0));
    }

    @Test
    void shouldCancelSubscription() {
        Subscription subscription = new Subscription();
        subscription.setId(10L);
        subscription.setStatus(SubscriptionStatus.ACTIVED);
        subscription.setEndDate(LocalDateTime.of(2030, 1, 1, 0, 0));

        when(repository.findById(10L)).thenReturn(Optional.of(subscription));

        Subscription result = service.cancelSubscription(10L);

        assertEquals(SubscriptionStatus.CANCELED, result.getStatus());
        assertNotNull(result.getEndDate());
        verify(repository).findById(10L);
    }

    @Test
    void shouldThrowWhenCancellingNonExistingSubscription() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                SubscriptionNotFoundException.class,
                () -> service.cancelSubscription(99L)
        );
    }

    @Test
    void shouldThrowWhenCancellingAlreadyCancelledSubscription() {
        Subscription subscription = new Subscription();
        subscription.setId(10L);
        subscription.setStatus(SubscriptionStatus.CANCELED);

        when(repository.findById(10L)).thenReturn(Optional.of(subscription));

        assertThrows(
                IllegalStateException.class,
                () -> service.cancelSubscription(10L)
        );
    }
}