package billing_core_api.service.validator;

import billing_core_api.domain.user.User;
import billing_core_api.exception.EmailAlreadyExistException;
import billing_core_api.exception.InvalidFieldException;
import billing_core_api.exception.RegistrationDuplicated;
import billing_core_api.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserValidator {

    private final UserRepository repository;

    public void validatePassword(String senha){
        if (senha == null || senha.isBlank()) {
            throw new InvalidFieldException("Password","Password cannot be blank");
        }
    }

    public void validateEmail(String email){
        if (repository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyExistException("Email already exists");
        }
    }

    public void validateName(String name){
        if (name == null || name.isBlank()) {
            throw new InvalidFieldException("Name","Name cannot be blank");
        }
    }

    public void validate(User user) {
        validateName(user.getName());
        validatePassword(user.getPassword());
        validateEmailNotTakenByAnotherUser(user);
    }

    private void validateEmailNotTakenByAnotherUser(User user){
        Optional<User> userWithSameEmail = repository.findByEmail(user.getEmail());

        boolean takenByAnotherUser = userWithSameEmail.isPresent()
                && (user.getId() == null || !user.getId().equals(userWithSameEmail.get().getId()));

        if (takenByAnotherUser) {
            throw new RegistrationDuplicated("Autor já cadastrado");
        }
    }
}
