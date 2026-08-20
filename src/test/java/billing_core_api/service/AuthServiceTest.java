package billing_core_api.service;

import billing_core_api.config.TokenProvider;
import billing_core_api.domain.user.RolesUser;
import billing_core_api.domain.user.User;
import billing_core_api.dto.auth.LoginRequestDTO;
import billing_core_api.dto.auth.ResponseAuthDTO;
import billing_core_api.dto.user.CreateUserDTO;
import billing_core_api.enums.RoleTypeEnum;
import billing_core_api.repository.RoleRepository;
import billing_core_api.repository.UserRepository;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository repository;
    @Mock PasswordEncoder encoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock RoleRepository roleRepository;
    @Mock TokenProvider tokenProvider;
    @Mock Authentication authentication;

    @InjectMocks AuthService service;

    private CreateUserDTO createUserDTO;
    private RolesUser role;

    @BeforeEach
    void setUp() {
        createUserDTO = new CreateUserDTO(
                "Caio",
                "caio@email.com",
                "123456"
        );
        role = RolesUser.builder().id(1).name(RoleTypeEnum.ROLE_USER.name()).build();

        ReflectionTestUtils.setField(service, "expirationTime", 3600000L);
    }

    @Test
    void shouldRegisterUserWithExistingRole() throws Exception {
        when(repository.findByEmail(createUserDTO.email())).thenReturn(Optional.empty());
        when(roleRepository.findByName(RoleTypeEnum.ROLE_USER.name())).thenReturn(Optional.of(role));
        when(encoder.encode("123456")).thenReturn("encoded");
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.register(createUserDTO);

        assertEquals("Caio", result.getName());
        assertEquals("caio@email.com", result.getEmail());
        assertEquals("encoded", result.getPassword());
        assertEquals(List.of(role), result.getRoles());

        verify(roleRepository).findByName(RoleTypeEnum.ROLE_USER.name());
        verify(roleRepository, never()).save(any());
        verify(repository).save(any(User.class));
    }

    @Test
    void shouldCreateDefaultRoleWhenRoleDoesNotExist() throws Exception {
        when(repository.findByEmail(createUserDTO.email())).thenReturn(Optional.empty());
        when(roleRepository.findByName(RoleTypeEnum.ROLE_USER.name())).thenReturn(Optional.empty());
        when(roleRepository.save(any(RolesUser.class))).thenReturn(role);
        when(encoder.encode("123456")).thenReturn("encoded");
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.register(createUserDTO);

        assertEquals(List.of(role), result.getRoles());
        verify(roleRepository).save(any(RolesUser.class));
        verify(repository).save(any(User.class));
    }

    @Test
    void shouldRejectDuplicatedEmail() {
        User existing = User.builder().email(createUserDTO.email()).build();
        when(repository.findByEmail(createUserDTO.email())).thenReturn(Optional.of(existing));

        assertThrows(
                BadRequestException.class,
                () -> service.register(createUserDTO)
        );

        verify(repository, never()).save(any(User.class));
        verify(roleRepository, never()).findByName(anyString());
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        LoginRequestDTO dto = new LoginRequestDTO("caio@email.com", "123456");

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(tokenProvider.generaToker(authentication)).thenReturn("jwt-token");

        ResponseAuthDTO response = service.login(dto);

        assertEquals("jwt-token", response.token());
        assertEquals(3600000L, response.expiresIn());
    }

    @Test
    void shouldTranslateBadCredentialsToBadRequestException() {
        LoginRequestDTO dto = new LoginRequestDTO("caio@email.com", "wrong");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThrows(BadRequestException.class, () -> service.login(dto));

        verify(tokenProvider, never()).generaToker(any());
    }

    @Test
    void shouldPropagateUnexpectedAuthenticationException() {
        LoginRequestDTO dto = new LoginRequestDTO("caio@email.com", "123456");

        RuntimeException exception = new RuntimeException("database down");
        when(authenticationManager.authenticate(any())).thenThrow(exception);

        RuntimeException thrown =
                assertThrows(RuntimeException.class, () -> service.login(dto));

        assertSame(exception, thrown);
    }
}
