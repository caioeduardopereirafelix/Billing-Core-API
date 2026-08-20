package billing_core_api.service;

import billing_core_api.domain.user.User;
import billing_core_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock UserRepository repository;
    @InjectMocks UserDetailsServiceImpl service;

    @Test
    void shouldLoadUserByEmail() {
        User user = User.builder().email("caio@email.com").build();
        when(repository.findByEmail("caio@email.com")).thenReturn(Optional.of(user));

        assertSame(user, service.loadUserByUsername("caio@email.com"));
    }

    @Test
    void shouldThrowWhenUserIsNotFound() {
        when(repository.findByEmail("missing@email.com")).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing@email.com")
        );
    }
}
