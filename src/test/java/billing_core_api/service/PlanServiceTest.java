package billing_core_api.service;

import billing_core_api.domain.BillingCycle;
import billing_core_api.domain.Plan;
import billing_core_api.dto.PlanRequest;
import billing_core_api.exception.PlanAlreadyExists;
import billing_core_api.exception.PlanNotFound;
import billing_core_api.repository.PlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlanServiceTest {

    @Mock
    private PlanRepository repository;

    @InjectMocks
    private PlanService service;

    @Test
    @DisplayName("Deve criar um plano com sucesso")
    void deveCriarPlano() {

        PlanRequest request = new PlanRequest(
                "Basic",
                "Plano básico",
                BigDecimal.valueOf(49.90),
                BillingCycle.MONTHLY
        );

        when(repository.existsByName("Basic"))
                .thenReturn(false);

        when(repository.save(any(Plan.class)))
                .thenAnswer(invocation -> {
                    Plan plan = invocation.getArgument(0);
                    plan.setId(1L);
                    return plan;
                });

        Plan plan = service.createPlan(request);

        assertNotNull(plan);
        assertEquals("Basic", plan.getName());
        assertEquals(BigDecimal.valueOf(49.90), plan.getPrice());
        assertTrue(plan.isActive());

        verify(repository).save(any(Plan.class));
    }

    @Test
    @DisplayName("Não deve permitir plano duplicado")
    void deveLancarExcecaoQuandoPlanoJaExiste() {

        PlanRequest request = new PlanRequest(
                "Basic",
                "Plano básico",
                BigDecimal.TEN,
                BillingCycle.MONTHLY
        );

        when(repository.existsByName("Basic"))
                .thenReturn(true);

        assertThrows(
                PlanAlreadyExists.class,
                () -> service.createPlan(request)
        );

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve Buscar Plano Por id")
    void deveBuscarPlanoPorId() {

        Plan plan = new Plan();
        plan.setId(1L);

        when(repository.findById(1L))
                .thenReturn(Optional.of(plan));

        Plan resultado = service.findById(1L);

        assertEquals(1L, resultado.getId());

        verify(repository).findById(1L);
    }

    @Test
    @DisplayName("Deve Lancar Excecao Quando Plano Nao Existe")
    void deveLancarExcecaoQuandoPlanoNaoExiste() {

        when(repository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                PlanNotFound.class,
                () -> service.findById(1L)
        );
    }

    @Test
    @DisplayName("Lista todos os planos")
    void deveListarTodosOsPlanos() {

        List<Plan> planos = List.of(
                new Plan(),
                new Plan()
        );

        when(repository.findAll())
                .thenReturn(planos);

        List<Plan> resultado = service.listAll();

        assertEquals(2, resultado.size());

        verify(repository).findAll();
    }

    @Test
    @DisplayName("Deve desativar Plano")
    void deveDesativarPlano() {

        Plan plan = new Plan();
        plan.setId(1L);
        plan.setActive(true);

        when(repository.findById(1L))
                .thenReturn(Optional.of(plan));

        Plan resultado = service.disabledPlan(1L);

        assertFalse(resultado.isActive());
    }

    @Test
    @DisplayName("Deve lancar excecao quando plano ja esta desativado")
    void deveLancarExcecaoQuandoPlanoJaEstaDesativado() {

        Plan plan = new Plan();
        plan.setId(1L);
        plan.setActive(false);

        when(repository.findById(1L))
                .thenReturn(Optional.of(plan));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.disabledPlan(1L)
        );
    }

    @Test
    @DisplayName("Atualizar preco com sucesso")
    void deveAtualizarPreco() {

        Plan plan = new Plan();
        plan.setId(1L);
        plan.setPrice(BigDecimal.TEN);

        when(repository.findById(1L))
                .thenReturn(Optional.of(plan));

        when(repository.save(any(Plan.class)))
                .thenReturn(plan);

        Plan resultado = service.putPrice(
                1L,
                BigDecimal.valueOf(99.90)
        );

        assertEquals(
                BigDecimal.valueOf(99.90),
                resultado.getPrice()
        );

        verify(repository).save(plan);
    }

    @Test
    @DisplayName("Lanca excecao para preco menor ou igual a zero")
    void deveLancarExcecaoQuandoPrecoForInvalido() {

        Plan plan = new Plan();

        when(repository.findById(1L))
                .thenReturn(Optional.of(plan));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.putPrice(
                        1L,
                        BigDecimal.ZERO
                )
        );

        verify(repository, never()).save(any());
    }

}
