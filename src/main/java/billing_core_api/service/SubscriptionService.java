package billing_core_api.service;

import billing_core_api.domain.Subscription;
import billing_core_api.dto.SubscriptionRequest;
import billing_core_api.repository.SubscriptionRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    SubscriptionRepository repository;


    public Subscription createSubscription(SubscriptionRequest request){

    }
}
