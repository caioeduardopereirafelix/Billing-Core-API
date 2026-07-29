package billing_core_api.controller;

import billing_core_api.domain.Plan;
import billing_core_api.dto.PlanRequest;
import billing_core_api.dto.PlanResponse;
import billing_core_api.repository.PlanRepository;
import billing_core_api.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
