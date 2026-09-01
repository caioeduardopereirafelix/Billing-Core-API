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

        var response = new SubscriptionResponse(subscription.getCustomerName(),
                subscription.getCustomerName(), subscription.getPlan().getName(), subscription.getStatus().toString());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> findById(@Valid @PathVariable Long id){

        var authenticatedUser = securityUtils.getAuthenticatedUser();

        if (!authenticatedUser.getId().toString().equals(id)
                && !authenticatedUser.getRoles().equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var subscription = service.buscarPorId(id);

        var response = new SubscriptionResponse(subscription.getCustomerName(),
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

        var authenticatedUser = securityUtils.getAuthenticatedUser();

        if (!authenticatedUser.getId().toString().equals(id)
                && !authenticatedUser.getRoles().equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Subscription subscription = service.cancelSubscription(id);

        SubscriptionResponse response = new SubscriptionResponse(subscription.getCustomerEmail(),subscription.getCustomerName(),subscription.getPlan().getName(), subscription.getStatus().toString());

        return ResponseEntity.ok(response);
    }


}
