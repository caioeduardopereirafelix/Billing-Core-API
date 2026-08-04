package billing_core_api.service;

import billing_core_api.domain.Plan;
import billing_core_api.domain.Subscription;
import billing_core_api.domain.SubscriptionStatus;
import billing_core_api.dto.SubscriptionRequest;
import billing_core_api.exception.PlanNotFound;
import billing_core_api.exception.SubscriptionNotFoundException;
import billing_core_api.messaging.SubscriptionEventPublisher;
import billing_core_api.messaging.event.SubscriptionCreatedEvent;
import billing_core_api.repository.PlanRepository;
import billing_core_api.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository repository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SubscriptionEventPublisher eventPublisher;

    @InjectMocks
    private SubscriptionService service;

    @Test
    @DisplayName("Deve Criar Uma Subscription com sucesso")
    void deveCriarUmaSubscription() {

        Plan plan = new Plan();
        plan.setId(1L);
        plan.setName("Basic");
        plan.setPrice(BigDecimal.valueOf(49.90));

        SubscriptionRequest request =
                new SubscriptionRequest(
                        "caio@email.com",
                        "Caio",
                        1L
                );

        when(planRepository.findById(1L))
                .thenReturn(Optional.of(plan));

        when(repository.save(any(Subscription.class)))
                .thenAnswer(invocation -> {
                    Subscription subscription = invocation.getArgument(0);
                    subscription.setId(10L);
                    return subscription;
                });

        Subscription subscription =
                service.createSubscription(request);

        assertNotNull(subscription);

        assertEquals("Caio", subscription.getCustomerName());

        assertEquals("caio@email.com",
                subscription.getCustomerEmail());

        assertEquals(plan, subscription.getPlan());

        assertEquals(SubscriptionStatus.ACTIVED,
                subscription.getStatus());

        verify(repository).save(any(Subscription.class));

        verify(eventPublisher)
                .publishSubscriptionCreated(any(SubscriptionCreatedEvent.class));
    }


    @DisplayName("Deve lancar excecao quando plano nao existe")
    @Test
    void deveLancarExcecaoQuandoPlanoNaoExiste() {

        SubscriptionRequest request =
                new SubscriptionRequest(
                        "teste@email.com",
                        "Caio",
                        99L
                );

        when(planRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                PlanNotFound.class,
                () -> service.createSubscription(request)
        );

        verify(repository, never()).save(any());

        verify(eventPublisher, never())
                .publishSubscriptionCreated(any());
    }

    @Test
    @DisplayName("Buscar assinatura existente")
    void deveBuscarSubscriptionPorId() {

        Subscription subscription = new Subscription();
        subscription.setId(1L);

        when(repository.findById(1L))
                .thenReturn(Optional.of(subscription));

        Subscription resultado =
                service.buscarPorId(1L);

        assertEquals(1L, resultado.getId());

        verify(repository).findById(1L);
    }

    @Test
    @DisplayName("Busca assinatura inexistente")
    void deveLancarExcecaoAoBuscarSubscriptionInexistente() {

        when(repository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                SubscriptionNotFoundException.class,
                () -> service.buscarPorId(1L)
        );
    }

    @Test
    @DisplayName("Cancela assinatura existente")
    void deveCancelarSubscription() {

        Subscription subscription = new Subscription();
        subscription.setId(1L);
        subscription.setStatus(SubscriptionStatus.ACTIVED);

        when(repository.findById(1L))
                .thenReturn(Optional.of(subscription));

        Subscription resultado =
                service.cancelSubscription(1L);

        assertEquals(
                SubscriptionStatus.CANCELED,
                resultado.getStatus()
        );

        verify(repository).findById(1L);
    }

    @Test
    @DisplayName("Cancela assinatura inexistente")
    void deveLancarExcecaoAoCancelarSubscriptionInexistente() {

        when(repository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                SubscriptionNotFoundException.class,
                () -> service.cancelSubscription(1L)
        );
    }
}
