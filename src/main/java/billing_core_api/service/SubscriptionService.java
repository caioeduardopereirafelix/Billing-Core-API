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
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository repository;
    private final PlanRepository planRepository;
    private final SubscriptionEventPublisher eventPublisher;


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

        String correlationID = UUID.randomUUID().toString();
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                saved.getId(),
                saved.getCustomerEmail(),
                saved.getCustomerName(),
                saved.getPlan().getName(),
                correlationID
        );

        eventPublisher.publishSubscriptionCreated(event);

        return saved;
    }

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
        return repository.save(subscription);
    }
}
