package billing_core_api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.HttpSecurityDsl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter filter;

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)throws Exception{

        return http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                    response.setStatus(HttpStatus.FORBIDDEN.value());
                                }))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()

                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()

                        .requestMatchers(HttpMethod.GET, "/plan/**").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/plan/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/plan/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/plan/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/plan/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/subscription").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/subscription/{id}").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/subscription").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/subscription/{id}/cancel").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/user/{userId}").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/user/{userId}").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/user/{userId}").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .build();

    }
}
