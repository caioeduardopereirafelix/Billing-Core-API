package billing_core_api.controller;

import billing_core_api.dto.plan.PlanRequest;
import billing_core_api.dto.plan.PlanResponse;
import billing_core_api.dto.plan.UpdatePlanRequest;
import billing_core_api.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/plan")
public class PlanController {

    private final PlanService service;

    @PostMapping
    public ResponseEntity<PlanResponse> createPlan(@Valid @RequestBody PlanRequest request){
        var plan = service.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PlanResponse.from(plan));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> findById(@PathVariable Long id){
        return ResponseEntity.ok(PlanResponse.from(service.findById(id)));
    }

    @GetMapping
    public ResponseEntity<List<PlanResponse>> listAll(){
        var response = service.listAll()
                .stream()
                .map(PlanResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<PlanResponse> cancelPlan(@PathVariable Long id){
        return ResponseEntity.ok(PlanResponse.from(service.disabledPlan(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanResponse> changePrice(@PathVariable Long id, @Valid @RequestBody UpdatePlanRequest planRequest){
        return ResponseEntity.ok(PlanResponse.from(service.putPlan(id, planRequest)));
    }
}
