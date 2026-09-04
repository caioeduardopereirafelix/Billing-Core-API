package billing_core_api.service;

import billing_core_api.domain.user.User;
import billing_core_api.dto.user.UpdateUserDTO;
import billing_core_api.exception.UserNotFound;
import billing_core_api.repository.UserRepository;
import billing_core_api.service.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final UserValidator userValidator;

    public void save(User user){
        repository.save(user);
    }

    public Optional<User> findById(UUID id){
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public User getById(UUID id){
        return repository.findById(id)
                .orElseThrow(() -> new UserNotFound("User " + id + " not found"));
    }

    @Transactional
    public User deposit(UUID userId, BigDecimal amount){
        var user = repository.findById(userId)
                .orElseThrow(() -> new UserNotFound("User " + userId + " not found"));
        user.setBalance(user.getBalance().add(amount));
        return repository.save(user);
    }

    public void deleteUser(User user) {
        repository.delete(user);
    }

    public User updateUser(UUID id, UpdateUserDTO request) {
        var user = repository.findById(id)
                .orElseThrow(() -> new UserNotFound("User " + id + " not found"));

        user.setName(request.name());
        user.setEmail(request.email());

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(encoder.encode(request.password()));
        }

        userValidator.validate(user);

        return repository.save(user);
    }
}
