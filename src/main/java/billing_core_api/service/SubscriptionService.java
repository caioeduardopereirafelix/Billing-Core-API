package billing_core_api.service;

import billing_core_api.domain.Subscription;
import billing_core_api.domain.SubscriptionStatus;
import billing_core_api.dto.SubscriptionRequest;
import billing_core_api.dto.SubscriptionResponse;
import billing_core_api.exception.PlanNotFound;
import billing_core_api.exception.SubscriptionNotFoundException;
import billing_core_api.messaging.SubscriptionEventPublisher;
import billing_core_api.messaging.event.SubscriptionCreatedEvent;
import billing_core_api.repository.PlanRepository;
import billing_core_api.repository.SubscriptionRepository;
import jakarta.transaction.Transactional;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository repository;
    private final PlanRepository planRepository;
    private final SubscriptionEventPublisher eventPublisher;
    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);


    public Subscription createSubscription(SubscriptionRequest request){
        var plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new PlanNotFound(request.planId().toString()));

        var subscription = new Subscription();
        subscription.setCustomerName(request.customerName());
        subscription.setCustomerEmail(request.customerEmail());
        subscription.setPlan(plan);
        subscription.setAmount(plan.getPrice());
        subscription.setStartDate(LocalDate.now());
        subscription.setStatus(SubscriptionStatus.ACTIVED);
        subscription.setCreatedDate(LocalDate.now());

        var saved = repository.save(subscription);

        //criacao mensagem para Notification-Worker

        String correlationId = UUID.randomUUID().toString();
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                UUID.randomUUID(),
                saved.getId(),
                saved.getCustomerEmail(),
                saved.getCustomerName(),
                saved.getPlan().getName(),
                correlationId
        );

        log.info("PUBLICANDO EVENT: {}", event.eventId());
        log.info("CorrelationId enviado: {}", event.correlationID());
        eventPublisher.publishSubscriptionCreated(event);

        return saved;
    }

    @Cacheable(value = "subscriptions", key = "#id")
    public Subscription buscarPorId(Long id){
       return repository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription Not Found"));
    }

    public List<Subscription> listAll(){
        List<Subscription> all = repository.findAll();
        return all;
    }

    public Subscription cancelSubscription(Long id){
        var subscription = repository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException(id.toString()));

        subscription.cancel();

        return subscription;
    }

}
