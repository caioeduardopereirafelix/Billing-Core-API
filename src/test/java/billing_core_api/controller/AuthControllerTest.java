package billing_core_api.controller;

import billing_core_api.config.JwtAuthenticationFilter;
import billing_core_api.config.SecurityConfig;
import billing_core_api.config.TokenProvider;
import billing_core_api.domain.user.User;
import billing_core_api.dto.auth.ResponseAuthDTO;
import billing_core_api.service.AuthService;
import billing_core_api.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AuthService authService;

    // Collaborators of JwtAuthenticationFilter, imported so the real filter runs as a pass-through.
    @MockitoBean
    TokenProvider tokenProvider;

    @MockitoBean
    UserDetailsServiceImpl userDetailsService;

    @Test
    void shouldAllowLoginWithoutAuthentication() throws Exception {
        when(authService.login(any())).thenReturn(new ResponseAuthDTO("a-token", 3600000L));

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"caio@email.com","password":"secret1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("a-token"));
    }

    @Test
    void shouldAllowRegisterWithoutAuthentication() throws Exception {
        when(authService.register(any())).thenReturn(User.builder().build());

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Caio","email":"caio@email.com","password":"secret1"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldStillProtectOtherEndpoints() throws Exception {
        mvc.perform(get("/user/123"))
                .andExpect(status().isUnauthorized());
    }
}
