package billing_core_api.service;

import billing_core_api.domain.plan.Plan;
import billing_core_api.dto.plan.PlanRequest;
import billing_core_api.dto.plan.UpdatePlanRequest;
import billing_core_api.exception.PlanAlreadyExists;
import billing_core_api.exception.PlanNotFound;
import billing_core_api.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository repository;

    public Plan createPlan(PlanRequest request){

        if (repository.existsByName(request.namePlan())){
            throw new PlanAlreadyExists("Already exist plan with that name");
        }

        Plan plan = new Plan();

        plan.setName(request.namePlan());
        plan.setDescription(request.description());
        plan.setPrice(request.price());
        plan.setBillingCycle(request.billingCycle());
        plan.setActive(true);

        return repository.save(plan);
    }

    @Cacheable(value = "plan", key = "#id")
    public Plan findById(Long id){

        System.out.println("BUSCANDO PLANO NO BANCO: " + id);

        var plan = repository.findById(id)
                .orElseThrow(() -> new PlanNotFound("Plan not exist, try again later"));

        return plan;
    }

    @Cacheable(value = "plan")
    public List<Plan> listAll(){

        List<Plan> all = repository.findAll();
        return all;
    }

    @CachePut(value = "plan", key = "#id")
    public Plan disabledPlan(Long id){

        var plan = repository.findById(id)
                .orElseThrow(() -> new PlanNotFound(id.toString()));

        if (!plan.getActive()){
            throw new IllegalArgumentException("Plan already disabled");
        }

        plan.setActive(false);

        return repository.save(plan);
    }

    @CachePut(value = "plan", key = "#id")
    public Plan putPlan(Long id, UpdatePlanRequest planRequest) {

        Plan plan = repository.findById(id)
                .orElseThrow(() -> new PlanNotFound(id.toString()));

        plan.setName(planRequest.newNamePlan());
        plan.setPrice(planRequest.newPrice());
        plan.setDescription(planRequest.newDescription());
        plan.setBillingCycle(planRequest.newBillingCycle());

        return repository.save(plan);
    }
}
