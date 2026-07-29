package billing_core_api.service;

import billing_core_api.domain.Plan;
import billing_core_api.dto.PlanRequest;
import billing_core_api.exception.PlanNotFound;
import billing_core_api.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {

    PlanRepository repository;

    public Plan createPlan(PlanRequest request){

        if (repository.existsByName(request.namePlan())){
            throw new IllegalArgumentException("Already exist plan with that name");
        }

        Plan plan = new Plan();

        plan.setName(request.namePlan());
        plan.setDescription(request.description());
        plan.setPrice(request.price());
        plan.setBillingCycle(request.billingCycle());
        plan.setActive(true);

        return repository.save(plan);
    }

    public Plan findById(Long id){

       return repository.findById(id)
                .orElseThrow(() -> new PlanNotFound("Plan not exist, try again later"));
    }

    public List<Plan> listAll(){

        List<Plan> all = repository.findAll();
        return all;
    }

    public Plan disabledPlan(Long id){

        var plan = repository.findById(id)
                .orElseThrow(() -> new PlanNotFound(id.toString()));

        if (!plan.isActive()){
            throw new IllegalArgumentException("Plan already disabled");
        }

        plan.setActive(false);

        return repository.save(plan);
    }

    public Plan atualizarPreco(Long id, BigDecimal novoPreco) {

        Plan plan = repository.findById(id)
                .orElseThrow(() -> new PlanNotFound(id.toString()));

        if (novoPreco == null || novoPreco.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O preço deve ser maior que zero.");
        }

        plan.setPrice(novoPreco);

        return repository.save(plan);
    }
}
