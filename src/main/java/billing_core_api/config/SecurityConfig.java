package billing_core_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.HttpSecurityDsl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)throws Exception{

        return http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        exception -> exception.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                    response.setStatus(HttpStatus.FORBIDDEN.value());
                                }))
                .authorizeHttpRequests(auth -> auth
                                .requestMatchers(HttpMethod.POST,"/v1/auth","/v1/auth/**")
                                .permitAll()

                                .requestMatchers(HttpMethod.POST,"/user","/user/**")
                                .permitAll()

                                .requestMatchers(HttpMethod.GET, "/user", "user/**")
                                .permitAll()

                                .requestMatchers(HttpMethod.GET, "/transaction","/transaction/**")
                                .hasAnyRole("ADMIN", "USER")

                                .requestMatchers(HttpMethod.DELETE, "/transaction", "/transaction/**")
                                .hasAnyRole("ADMIN", "USER")

                                .requestMatchers(HttpMethod.POST, "/transaction", "/transaction/**")
                                .hasAnyRole("ADMIN", "USER")

                                .requestMatchers(HttpMethod.PUT, "/transaction", "/transaction/**")
                                .hasAnyRole("ADMIN", "USER")
                                .anyRequest().authenticated()
                ).build();

    }
}
