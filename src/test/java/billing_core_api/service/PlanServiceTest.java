package billing_core_api.service;

import billing_core_api.repository.PlanRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PlanServiceTest {

    @Mock
    private PlanRepository repository;

    @InjectMocks
    private PlanService service;
}
