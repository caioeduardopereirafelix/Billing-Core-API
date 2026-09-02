package billing_core_api.service;

import billing_core_api.config.TokenProvider;
import billing_core_api.domain.user.User;
import billing_core_api.dto.auth.LoginRequestDTO;
import billing_core_api.dto.auth.ResponseAuthDTO;
import billing_core_api.dto.user.CreateUserDTO;
import billing_core_api.enums.RoleTypeEnum;
import billing_core_api.repository.RoleRepository;
import billing_core_api.repository.UserRepository;
import billing_core_api.service.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
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

    /**
     * Single entry point for user creation: e-mail uniqueness check, default role
     * assignment, password hashing and audit timestamps all happen here.
     */
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
                .roles(List.of(role))
                .build();

        user.setCreatedDate(LocalDate.now());
        user.setLastModifiedDate(Instant.now());

        return repository.save(user);
    }

    public ResponseAuthDTO login(LoginRequestDTO dto) throws BadRequestException {

        try {

            //authentication provider -> UserDetailsService -> password matches do passwordencoder -> autenticado -> Authentication
            var authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(dto.email(), dto.password()));
            String token = tokenProvider.generaToker(authentication);

            return new ResponseAuthDTO(token, expirationTime);

        }catch (BadCredentialsException e){
            throw new BadRequestException("Invalid Credencials");
        } catch (Exception e) {
            throw e;
        }
    }


}
