package billing_core_api.service;

import billing_core_api.domain.subscription.Subscription;
import billing_core_api.domain.user.User;
import billing_core_api.enums.SubscriptionStatus;
import billing_core_api.dto.subscription.SubscriptionRequest;
import billing_core_api.exception.BusinessRuleException;
import billing_core_api.exception.InsufficientBalanceException;
import billing_core_api.exception.PlanNotFound;
import billing_core_api.exception.SubscriptionNotFoundException;
import billing_core_api.exception.UserNotFound;
import billing_core_api.messaging.SubscriptionEventPublisher;
import billing_core_api.messaging.event.SubscriptionCreatedEvent;
import billing_core_api.repository.PlanRepository;
import billing_core_api.repository.SubscriptionRepository;
import billing_core_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository repository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final SubscriptionEventPublisher eventPublisher;
    private final CalculateAndSubscription calculateEndSubscription;
    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    @Transactional
    public Subscription createSubscription(SubscriptionRequest request, User user){
        var plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new PlanNotFound(request.planId().toString()));

        if (!plan.getActive()) {
            throw new BusinessRuleException("Plan " + plan.getName() + " is not available");
        }


        var subscriber = userRepository.findById(user.getId())
                .orElseThrow(() -> new UserNotFound(user.getId().toString()));

        if (subscriber.getBalance().compareTo(plan.getPrice()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance: plan costs " + plan.getPrice()
                            + ", current balance is " + subscriber.getBalance());
        }
        subscriber.setBalance(subscriber.getBalance().subtract(plan.getPrice()));

        var subscription = new Subscription();
        subscription.setCustomerName(subscriber.getName());
        subscription.setCustomerEmail(subscriber.getEmail());
        subscription.setUser(subscriber);
        subscription.setPlan(plan);
        subscription.setAmount(plan.getPrice());
        subscription.setStartDate(LocalDate.now());

        LocalDateTime endDate =
                calculateEndSubscription.calculateEndDate(subscription.getStartDate(),
                                                            plan.getBillingCycle());
        subscription.setEndDate(endDate);
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

    @Transactional(readOnly = true)
    public Subscription buscarPorId(Long id){
       return repository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription Not Found"));
    }

    @Transactional(readOnly = true)
    public List<Subscription> listByUser(UUID userId){
        return repository.findByUser_IdOrderByStartDateDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Subscription> listAll(){
        return repository.findAll();
    }

    @Transactional
    public Subscription cancelSubscription(Long id){
        var subscription = repository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException(id.toString()));
        subscription.cancel();

        return subscription;
    }

}
