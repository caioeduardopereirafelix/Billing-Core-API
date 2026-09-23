package billing_core_api.controller;

import billing_core_api.config.AuthRateLimitFilter;
import billing_core_api.config.JwtAuthenticationFilter;
import billing_core_api.config.RateLimiter;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AuthRateLimitFilter.class, RateLimiter.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "rate-limit.auth.max-requests=2",
        "rate-limit.auth.window-seconds=60"
})
class AuthRateLimitFilterTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    TokenProvider tokenProvider;

    @MockitoBean
    UserDetailsServiceImpl userDetailsService;

    private static final String LOGIN_BODY = """
            {"email":"caio@email.com","password":"secret1"}
            """;

    @Test
    void blocksLoginAfterExceedingTheLimit() throws Exception {
        when(authService.login(any())).thenReturn(new ResponseAuthDTO("a-token", 3600000L));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void throttlesRegisterAndLoginIndependently() throws Exception {
        when(authService.login(any())).thenReturn(new ResponseAuthDTO("a-token", 3600000L));
        when(authService.register(any())).thenReturn(User.builder().name("Caio").email("caio@email.com").build());

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk());

        // /auth/register has its own bucket and is unaffected by /auth/login's usage
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Caio","email":"caio@email.com","password":"secret1"}
                        """))
                .andExpect(status().isCreated());
    }
}
