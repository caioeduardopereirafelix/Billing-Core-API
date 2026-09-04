package billing_core_api.service;

import billing_core_api.config.TokenProvider;
import billing_core_api.domain.user.User;
import billing_core_api.dto.auth.LoginRequestDTO;
import billing_core_api.dto.auth.ResponseAuthDTO;
import billing_core_api.dto.user.CreateUserDTO;
import billing_core_api.enums.RoleTypeEnum;
import billing_core_api.exception.InvalidCredentialsException;
import billing_core_api.repository.RoleRepository;
import billing_core_api.repository.UserRepository;
import billing_core_api.service.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final RoleRepository roleRepository;
    private final TokenProvider tokenProvider;
    private final UserValidator userValidator;

    @Value("${jwt.expiration}")
    private Long expirationTime;


    @Transactional
    public User register(CreateUserDTO dto) {

        userValidator.validateEmail(dto.email());

        var role = roleRepository.findByName(RoleTypeEnum.ROLE_USER.name())
                .orElseThrow(() -> new IllegalStateException(
                        "Default role " + RoleTypeEnum.ROLE_USER.name()
                                + " is missing - check the Flyway seed (V2__create_users_and_roles_tables.sql)."));

        User user = User.builder()
                .name(dto.name())
                .email(dto.email())
                .password(encoder.encode(dto.password()))
                .roles(new ArrayList<>(List.of(role)))
                .build();

        user.setCreatedDate(LocalDate.now());
        user.setLastModifiedDate(Instant.now());

        return repository.save(user);
    }

    public ResponseAuthDTO login(LoginRequestDTO dto) {

        try {
            // authentication provider -> UserDetailsService -> PasswordEncoder.matches -> Authentication
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.email(), dto.password()));

            return new ResponseAuthDTO(tokenProvider.generaToker(authentication), expirationTime);

        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }


}
