package billing_core_api.controller;

import billing_core_api.config.SecurityUtils;
import billing_core_api.domain.subscription.Subscription;
import billing_core_api.dto.subscription.SubscriptionRequest;
import billing_core_api.dto.subscription.SubscriptionResponse;
import billing_core_api.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService service;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(@Valid @RequestBody SubscriptionRequest subscriptionRequest){
        var authenticatedUser = securityUtils.getAuthenticatedUser();
        var subscription = service.createSubscription(subscriptionRequest, authenticatedUser);

        var response = new SubscriptionResponse(subscription.getCustomerEmail(),
                subscription.getCustomerName(), subscription.getPlan().getName(), subscription.getStatus().toString());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> findById(@PathVariable Long id){

        var subscription = service.buscarPorId(id);
        securityUtils.requireOwnerOrAdmin(subscription.getUser().getId());

        var response = new SubscriptionResponse(subscription.getCustomerEmail(),
                subscription.getCustomerName(), subscription.getPlan().getName(), subscription.getStatus().toString());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> listAll(){
        var response = service.listAll()
                .stream()
                .map(subscription -> new SubscriptionResponse(
                        subscription.getCustomerEmail(),
                        subscription.getCustomerName(),
                        subscription.getPlan().getName(),
                        subscription.getStatus().toString()
                ))
                .toList();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(@PathVariable Long id) {

        securityUtils.requireOwnerOrAdmin(service.buscarPorId(id).getUser().getId());

        Subscription subscription = service.cancelSubscription(id);

        SubscriptionResponse response = new SubscriptionResponse(subscription.getCustomerEmail(),subscription.getCustomerName(),subscription.getPlan().getName(), subscription.getStatus().toString());

        return ResponseEntity.ok(response);
    }


}
