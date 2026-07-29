package billing_core_api.controller;

import billing_core_api.dto.SubscriptionRequest;
import billing_core_api.dto.SubscriptionResponse;
import billing_core_api.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService service;

    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(@Valid @RequestBody SubscriptionRequest subscriptionRequest){
        var subscription = service.createSubscription(subscriptionRequest);

        var response = new SubscriptionResponse(subscription.getCustomerName(),
                subscription.getCustomerName());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


}
