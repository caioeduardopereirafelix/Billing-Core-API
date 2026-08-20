package billing_core_api.service;

import billing_core_api.config.TokenProvider;
import billing_core_api.domain.user.RolesUser;
import billing_core_api.domain.user.User;
import billing_core_api.dto.auth.LoginRequestDTO;
import billing_core_api.dto.auth.ResponseAuthDTO;
import billing_core_api.dto.user.CreateUserDTO;
import billing_core_api.repository.RoleRepository;
import billing_core_api.enums.RoleTypeEnum;
import billing_core_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final RoleRepository rolesUserRespository;
    private final TokenProvider tokenProvider;
    @Value("${jwt.expiration}")
    private Long expirationTime;


    public User register(CreateUserDTO dto) throws BadRequestException {

        var login = repository.findByEmail(dto.email())
                .orElse(null);

        if (login != null){
            throw new BadRequestException("User already registered with this Email");
        }

        var role = rolesUserRespository.findByName(RoleTypeEnum.ROLE_USER.name())
                .orElseGet(() -> rolesUserRespository.save(RolesUser.builder()
                        .name(RoleTypeEnum.ROLE_USER.name()).build()));

        User user = User.builder()
                .name(dto.name())
                .email(dto.email())
                .roles(List.of(role))
                .password(encoder.encode(dto.password()))
                .build();

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
