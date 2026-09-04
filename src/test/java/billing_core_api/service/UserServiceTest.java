package billing_core_api.service;

import billing_core_api.domain.user.User;
import billing_core_api.dto.user.UpdateUserDTO;
import billing_core_api.exception.UserNotFound;
import billing_core_api.repository.UserRepository;
import billing_core_api.service.validator.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository repository;
    @Mock PasswordEncoder encoder;
    @Mock UserValidator validator;

    @InjectMocks UserService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .name("Caio")
                .email("caio@email.com")
                .password("old")
                .build();
    }

    @Test
    void shouldFindUserById() {
        UUID id = user.getId();
        when(repository.findById(id)).thenReturn(Optional.of(user));

        assertEquals(Optional.of(user), service.findById(id));
    }

    @Test
    void depositAddsToTheBalanceAndPersists() {
        user.setBalance(new BigDecimal("10.00"));
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);

        User result = service.deposit(user.getId(), new BigDecimal("40.50"));

        assertEquals(new BigDecimal("50.50"), result.getBalance());
        verify(repository).save(user);
    }

    @Test
    void depositOnAMissingUserThrowsUserNotFound() {
        UUID missing = UUID.randomUUID();
        when(repository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(UserNotFound.class, () -> service.deposit(missing, new BigDecimal("10")));
        verify(repository, never()).save(any());
    }

    @Test
    void shouldReturnEmptyWhenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertTrue(service.findById(id).isEmpty());
    }

    @Test
    void shouldDeleteUser() {
        service.deleteUser(user);

        verify(repository).delete(user);
    }

    @Test
    void shouldUpdateUserWithoutChangingPasswordWhenPasswordIsNull() {
        UpdateUserDTO dto = new UpdateUserDTO("New Name", "new@email.com", null);
        String oldPassword = user.getPassword();

        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);

        User result = service.updateUser(user.getId(), dto);

        assertSame(user, result);
        assertEquals("New Name", user.getName());
        assertEquals("new@email.com", user.getEmail());
        assertEquals(oldPassword, user.getPassword());
        verify(encoder, never()).encode(anyString());
        verify(validator).validate(user);
        verify(repository).save(user);
    }

    @Test
    void shouldUpdateUserWithoutChangingPasswordWhenPasswordIsBlank() {
        UpdateUserDTO dto = new UpdateUserDTO("New Name", "new@email.com", "   ");

        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);

        service.updateUser(user.getId(), dto);

        verify(encoder, never()).encode(anyString());
        verify(repository).save(user);
    }

    @Test
    void shouldEncodeNewPasswordWhenUpdating() {
        UpdateUserDTO dto = new UpdateUserDTO("New Name", "new@email.com", "newPassword");

        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        when(encoder.encode("newPassword")).thenReturn("encoded-new");
        when(repository.save(user)).thenReturn(user);

        service.updateUser(user.getId(), dto);

        assertEquals("encoded-new", user.getPassword());
        verify(encoder).encode("newPassword");
        verify(repository).save(user);
    }

    @Test
    void shouldFailUpdateWhenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        UpdateUserDTO dto = new UpdateUserDTO("New Name", "new@email.com", null);

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFound.class, () -> service.updateUser(id, dto));

        verify(repository, never()).save(any());
    }
}