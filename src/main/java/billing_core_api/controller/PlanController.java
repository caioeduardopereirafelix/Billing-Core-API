package billing_core_api.controller;

import billing_core_api.dto.plan.PlanRequest;
import billing_core_api.dto.plan.PlanResponse;
import billing_core_api.repository.PlanRepository;
import billing_core_api.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/plan")
public class PlanController {

    private final PlanService service;
    private final PlanRepository repository;

    @PostMapping
    public ResponseEntity<PlanResponse> createPlan(@Valid @RequestBody PlanRequest request){
        var planToSave = service.createPlan(request);

        var response = new PlanResponse(planToSave.getId(),
                planToSave.getName(),
                planToSave.getPrice(),
                planToSave.isActive());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> findById(@Valid @PathVariable Long id){
        var plan = service.findById(id);

        var response = new PlanResponse(plan.getId(),
                plan.getName(),
                plan.getPrice(),
                plan.isActive());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PlanResponse>> listAll(){
        var response = service.listAll()
                .stream()
                .map(plan -> new PlanResponse(plan.getId(),
                        plan.getName(),
                        plan.getPrice(),
                        plan.isActive())).toList();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<PlanResponse> cancelPlan(@Valid @PathVariable Long id){
        var plan = service.disabledPlan(id);

        var response = new PlanResponse(plan.getId(), plan.getName(), plan.getPrice(), plan.isActive());

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanResponse> changePrice(@Valid @PathVariable Long id, @RequestBody BigDecimal newPrice){
        var plan = service.putPrice(id, newPrice);

        var response = new PlanResponse(plan.getId(), plan.getName(), plan.getPrice(), plan.isActive());

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }
}
