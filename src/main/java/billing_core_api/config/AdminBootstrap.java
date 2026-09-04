package billing_core_api.config;

import billing_core_api.domain.user.User;
import billing_core_api.enums.RoleTypeEnum;
import billing_core_api.repository.RoleRepository;
import billing_core_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Component
@RequiredArgsConstructor
public class AdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void createAdminIfConfigured() {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("app.admin.email/password not set - skipping admin bootstrap");
            return;
        }
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin user {} already exists - nothing to do", adminEmail);
            return;
        }

        var adminRole = roleRepository.findByName(RoleTypeEnum.ROLE_ADMIN.name())
                .orElseThrow(() -> new IllegalStateException(
                        "Role " + RoleTypeEnum.ROLE_ADMIN.name()
                                + " is missing - check the Flyway seed (V2__create_users_and_roles_tables.sql)."));

        var admin = User.builder()
                .name("Administrator")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .roles(new ArrayList<>(List.of(adminRole)))
                .build();
        admin.setCreatedDate(LocalDate.now());
        admin.setLastModifiedDate(Instant.now());

        userRepository.save(admin);
        log.info("Bootstrapped admin user {}", adminEmail);
    }
}
