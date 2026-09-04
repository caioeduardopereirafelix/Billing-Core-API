package billing_core_api.config;

import billing_core_api.domain.user.RolesUser;
import billing_core_api.domain.user.User;
import billing_core_api.enums.RoleTypeEnum;
import billing_core_api.repository.RoleRepository;
import billing_core_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AdminBootstrap bootstrap;

    private final RolesUser adminRole =
            RolesUser.builder().id(2).name(RoleTypeEnum.ROLE_ADMIN.name()).build();

    @BeforeEach
    void configure() {
        ReflectionTestUtils.setField(bootstrap, "adminEmail", "admin@billing.local");
        ReflectionTestUtils.setField(bootstrap, "adminPassword", "raw-input");
    }

    @Test
    void createsAdminWhenConfiguredAndAbsent() {
        when(userRepository.findByEmail("admin@billing.local")).thenReturn(Optional.empty());
        when(roleRepository.findByName(RoleTypeEnum.ROLE_ADMIN.name())).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("raw-input")).thenReturn("hashed");

        bootstrap.createAdminIfConfigured();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("admin@billing.local");
        assertThat(saved.getValue().getPassword()).isEqualTo("hashed");
        assertThat(saved.getValue().getRoles()).extracting("name")
                .containsExactly(RoleTypeEnum.ROLE_ADMIN.name());
        assertThat(saved.getValue().getCreatedDate()).isNotNull();
    }

    @Test
    void skipsWhenEmailOrPasswordBlank() {
        ReflectionTestUtils.setField(bootstrap, "adminPassword", "");

        bootstrap.createAdminIfConfigured();

        verifyNoInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void skipsWhenAdminAlreadyExists() {
        when(userRepository.findByEmail("admin@billing.local"))
                .thenReturn(Optional.of(User.builder().email("admin@billing.local").build()));

        bootstrap.createAdminIfConfigured();

        verify(userRepository, never()).save(any());
        verifyNoInteractions(roleRepository, passwordEncoder);
    }

    @Test
    void failsLoudlyWhenAdminRoleSeedMissing() {
        when(userRepository.findByEmail("admin@billing.local")).thenReturn(Optional.empty());
        when(roleRepository.findByName(RoleTypeEnum.ROLE_ADMIN.name())).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> bootstrap.createAdminIfConfigured());

        verify(userRepository, never()).save(any());
    }
}
