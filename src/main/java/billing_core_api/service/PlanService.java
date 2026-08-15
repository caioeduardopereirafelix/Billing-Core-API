package billing_core_api.service;

import billing_core_api.domain.plan.Plan;
import billing_core_api.dto.plan.PlanRequest;
import billing_core_api.exception.PlanAlreadyExists;
import billing_core_api.exception.PlanNotFound;
import billing_core_api.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
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

    public List<Plan> listAll(){

        List<Plan> all = repository.findAll();
        return all;
    }

    @CacheEvict(value = "plan", key = "#id")
    public Plan disabledPlan(Long id){

        var plan = repository.findById(id)
                .orElseThrow(() -> new PlanNotFound(id.toString()));

        if (!plan.isActive()){
            throw new IllegalArgumentException("Plan already disabled");
        }

        plan.setActive(false);

        return plan;
    }

    public Plan putPrice(Long id, BigDecimal novoPreco) {

        Plan plan = repository.findById(id)
                .orElseThrow(() -> new PlanNotFound(id.toString()));

        if (novoPreco == null || novoPreco.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("The price must be greater than 0.");
        }

        plan.setPrice(novoPreco);

        return repository.save(plan);
    }
}
