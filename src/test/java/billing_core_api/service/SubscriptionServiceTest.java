package billing_core_api.service;

import billing_core_api.domain.plan.Plan;
import billing_core_api.domain.subscription.Subscription;
import billing_core_api.domain.user.User;
import billing_core_api.dto.subscription.SubscriptionRequest;
import billing_core_api.enums.BillingCycle;
import billing_core_api.enums.SubscriptionStatus;
import billing_core_api.exception.BusinessRuleException;
import billing_core_api.exception.InsufficientBalanceException;
import billing_core_api.exception.PlanNotFound;
import billing_core_api.exception.SubscriptionNotFoundException;
import billing_core_api.messaging.SubscriptionEventPublisher;
import billing_core_api.repository.PlanRepository;
import billing_core_api.repository.SubscriptionRepository;
import billing_core_api.repository.UserRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock SubscriptionRepository repository;
    @Mock PlanRepository planRepository;
    @Mock UserRepository userRepository;
    @Mock SubscriptionEventPublisher eventPublisher;
    @Mock CalculateAndSubscription calculateEndSubscription;

    @InjectMocks SubscriptionService service;

    private Plan plan;
    private SubscriptionRequest request;
    private User user;

    @BeforeEach
    void setUp() {
        plan = new Plan();
        plan.setId(1L);
        plan.setName("Premium");
        plan.setPrice(new BigDecimal("99.90"));
        plan.setBillingCycle(BillingCycle.MONTHLY);
        plan.setActive(true);

        request = new SubscriptionRequest(1L);

        user = User.builder()
                .id(UUID.randomUUID())
                .name("Caio Silva")
                .email("caio@email.com")
                .balance(new BigDecimal("1000.00"))
                .build();
    }

    @Test
    void shouldCreateSubscriptionUsingTheAuthenticatedUserAsCustomer() {
        LocalDate today = LocalDate.now();
        LocalDateTime end = today.plusMonths(1).atTime(23, 59, 59);

        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(calculateEndSubscription.calculateEndDate(today, BillingCycle.MONTHLY)).thenReturn(end);
        when(repository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        Subscription result = service.createSubscription(request, user);

        assertEquals(10L, result.getId());
        // customer identity is a snapshot taken from the account, not from the request
        assertEquals("Caio Silva", result.getCustomerName());
        assertEquals("caio@email.com", result.getCustomerEmail());
        assertSame(user, result.getUser());
        assertSame(plan, result.getPlan());
        assertEquals(new BigDecimal("99.90"), result.getAmount());
        assertEquals(today, result.getStartDate());
        assertEquals(end, result.getEndDate());
        assertEquals(SubscriptionStatus.ACTIVED, result.getStatus());
        assertNotNull(result.getCreatedDate());
        // the plan price is charged against the balance
        assertEquals(new BigDecimal("900.10"), user.getBalance());

        verify(repository).save(any(Subscription.class));
        verify(eventPublisher).publishSubscriptionCreated(any());
    }

    @Test
    void shouldRejectWhenBalanceIsBelowThePlanPrice() {
        user.setBalance(new BigDecimal("50.00"));
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(InsufficientBalanceException.class,
                () -> service.createSubscription(request, user));

        assertEquals(new BigDecimal("50.00"), user.getBalance()); // untouched
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishSubscriptionCreated(any());
    }

    @Test
    void shouldPublishEventCarryingTheAccountEmail() {
        LocalDate today = LocalDate.now();

        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(calculateEndSubscription.calculateEndDate(any(), eq(BillingCycle.MONTHLY)))
                .thenReturn(today.plusMonths(1).atTime(23, 59, 59));
        when(repository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        service.createSubscription(request, user);

        ArgumentCaptor<billing_core_api.messaging.event.SubscriptionCreatedEvent> captor =
                ArgumentCaptor.forClass(billing_core_api.messaging.event.SubscriptionCreatedEvent.class);
        verify(eventPublisher).publishSubscriptionCreated(captor.capture());

        assertEquals("caio@email.com", captor.getValue().customerEmail());
        assertEquals("Caio Silva", captor.getValue().customerName());
    }

    @Test
    void shouldThrowWhenPlanDoesNotExist() {
        when(planRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(PlanNotFound.class, () -> service.createSubscription(request, user));

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishSubscriptionCreated(any());
    }

    @Test
    void shouldPropagateEndDateCalculatorFailureAndNotPublishEvent() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(calculateEndSubscription.calculateEndDate(any(), eq(BillingCycle.MONTHLY)))
                .thenThrow(new IllegalStateException("calculation failure"));

        assertThrows(IllegalStateException.class, () -> service.createSubscription(request, user));

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
    void shouldListSubscriptionsOfAGivenUser() {
        Subscription s1 = new Subscription();
        s1.setId(1L);
        Subscription s2 = new Subscription();
        s2.setId(2L);
        when(repository.findByUser_IdOrderByStartDateDesc(user.getId())).thenReturn(List.of(s1, s2));

        List<Subscription> result = service.listByUser(user.getId());

        assertEquals(List.of(s1, s2), result);
        verify(repository).findByUser_IdOrderByStartDateDesc(user.getId());
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
    void shouldCancelSubscriptionRecordingTheCancellationDate() {
        LocalDateTime periodEnd = LocalDateTime.of(2030, 1, 1, 0, 0);
        Subscription subscription = new Subscription();
        subscription.setId(10L);
        subscription.setStatus(SubscriptionStatus.ACTIVED);
        subscription.setEndDate(periodEnd);

        when(repository.findById(10L)).thenReturn(Optional.of(subscription));

        Subscription result = service.cancelSubscription(10L);

        assertEquals(SubscriptionStatus.CANCELED, result.getStatus());
        assertNotNull(result.getCanceledAt());
        assertEquals(periodEnd, result.getEndDate()); // the contracted end is left untouched
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
                BusinessRuleException.class,
                () -> service.cancelSubscription(10L)
        );
    }
}
