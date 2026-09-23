package billing_core_api.service;

import billing_core_api.domain.user.User;
import billing_core_api.service.validator.UserValidator;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import billing_core_api.exception.EmailAlreadyExistException;
import billing_core_api.exception.InvalidFieldException;
import billing_core_api.exception.RegistrationDuplicated;
import billing_core_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserValidatorTest {

    @Mock UserRepository repository;
    @InjectMocks
    UserValidator validator;

    @Test
    void shouldRejectBlankPassword() {
        assertThrows(InvalidFieldException.class, () -> validator.validatePassword("   "));
    }

    @Test
    void shouldRejectNullPassword() {
        assertThrows(InvalidFieldException.class, () -> validator.validatePassword(null));
    }

    @Test
    void shouldAcceptNonBlankPassword() {
        assertDoesNotThrow(() -> validator.validatePassword("123456"));
    }

    @Test
    void shouldRejectExistingEmail() {
        when(repository.findByEmail("caio@email.com"))
                .thenReturn(Optional.of(User.builder().email("caio@email.com").build()));

        assertThrows(
                EmailAlreadyExistException.class,
                () -> validator.validateEmail("caio@email.com")
        );
    }

    @Test
    void shouldAcceptNewEmail() {
        when(repository.findByEmail("new@email.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validator.validateEmail("new@email.com"));
    }

    @Test
    void shouldRejectBlankName() {
        assertThrows(InvalidFieldException.class, () -> validator.validateName(" "));
    }

    @Test
    void shouldRejectNullName() {
        assertThrows(InvalidFieldException.class, () -> validator.validateName(null));
    }

    @Test
    void shouldAcceptNonBlankName() {
        assertDoesNotThrow(() -> validator.validateName("Caio"));
    }

    @Test
    void shouldRejectNewUserWhenEmailAlreadyExists() {
        User existing = User.builder()
                .id(UUID.randomUUID())
                .name("Existing")
                .password("hashed")
                .email("caio@email.com")
                .build();

        User newUser = User.builder()
                .name("Caio")
                .password("hashed")
                .email("caio@email.com")
                .build();

        when(repository.findByEmail("caio@email.com")).thenReturn(Optional.of(existing));

        assertThrows(RegistrationDuplicated.class, () -> validator.validate(newUser));
    }

    @Test
    void shouldAcceptNewUserWhenEmailDoesNotExist() {
        User newUser = User.builder().name("Caio").password("hashed").email("new@email.com").build();

        when(repository.findByEmail("new@email.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validator.validate(newUser));
    }

    @Test
    void shouldAcceptUpdateWhenEmailBelongsToSameUser() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).name("Caio").password("hashed").email("caio@email.com").build();

        when(repository.findByEmail("caio@email.com")).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> validator.validate(user));
    }

    @Test
    void shouldRejectUpdateWhenEmailBelongsToAnotherUser() {
        UUID currentId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        User current = User.builder().id(currentId).name("Caio").password("hashed").email("caio@email.com").build();
        User other = User.builder().id(otherId).name("Other").password("hashed").email("caio@email.com").build();

        when(repository.findByEmail("caio@email.com")).thenReturn(Optional.of(other));

        assertThrows(RegistrationDuplicated.class, () -> validator.validate(current));
    }

    @Test
    void shouldAcceptUpdateWhenNewEmailDoesNotBelongToAnyUser() {
        UUID currentId = UUID.randomUUID();
        User current = User.builder().id(currentId).name("Caio").password("hashed").email("new@email.com").build();

        when(repository.findByEmail("new@email.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validator.validate(current));
    }

    @Test
    void shouldRejectUserWithBlankNameOnValidate() {
        User user = User.builder().name(" ").password("hashed").email("caio@email.com").build();

        assertThrows(InvalidFieldException.class, () -> validator.validate(user));
        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectUserWithBlankPasswordOnValidate() {
        User user = User.builder().name("Caio").password(" ").email("caio@email.com").build();

        assertThrows(InvalidFieldException.class, () -> validator.validate(user));
        verifyNoInteractions(repository);
    }
}
