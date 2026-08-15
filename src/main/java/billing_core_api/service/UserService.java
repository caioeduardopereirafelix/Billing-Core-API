package billing_core_api.service;

import billing_core_api.domain.user.User;
import billing_core_api.domain.mapper.UserMapper;
import billing_core_api.dto.user.CreateUserDTO;
import billing_core_api.dto.user.UpdateUserDTO;
import billing_core_api.repository.UserRepository;
import billing_core_api.service.validator.UserValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder encoder;
    private final UserValidator userValidator;


    public User createUser(@Valid @RequestBody CreateUserDTO dto){

        var userMap = mapper.toUser(dto);

        userMap.setPassword(encoder.encode(dto.password()));
        userValidator.validate(userMap);
        userMap.setCreatedDate(LocalDate.now());
        userMap.setLastModifiedDate(Instant.now());

        return repository.save(userMap);
    }

    public void save(User user){
        repository.save(user);
    }

    public Optional<User> findById(UUID id){
        return repository.findById(id);
    }

    public void deleteUser(User user) {
        repository.delete(user);
    }

    public User updateUser(UUID id, UpdateUserDTO request) {
        var user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(request.name());
        user.setEmail(request.email());

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(encoder.encode(request.password()));
        }

        userValidator.validate(user);

        return repository.save(user);
    }
}

