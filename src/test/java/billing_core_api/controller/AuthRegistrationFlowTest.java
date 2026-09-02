package billing_core_api.controller;

import billing_core_api.domain.user.User;
import billing_core_api.enums.RoleTypeEnum;
import billing_core_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end coverage of the single registration entry point (POST /auth/register):
 * creation, validation, e-mail uniqueness, default role assignment and password
 * hashing, all the way through to a usable JWT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthRegistrationFlowTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void register_persistsHashedUserWithDefaultRole_thenLoginYieldsUsableToken() throws Exception {
        String email = "flow-" + System.nanoTime() + "@example.com";
        String password = "secret1";
        String payload = """
                {"name":"Flow User","email":"%s","password":"%s"}
                """.formatted(email, password);

        // 1. creation
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Flow User"))
                .andExpect(jsonPath("$.email").value(email));

        // 2. persisted state: hashed password, audit date, ROLE_USER assigned
        User saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, saved.getPassword())).isTrue();
        assertThat(saved.getCreatedDate()).isNotNull();
        assertThat(saved.getRoles())
                .extracting("name")
                .containsExactly(RoleTypeEnum.ROLE_USER.name());

        // 3. login with the new credentials returns a token
        MvcResult login = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token = com.jayway.jsonpath.JsonPath.read(
                login.getResponse().getContentAsString(), "$.token");

        // 4. token authenticates and carries ROLE_USER (GET /plan requires it)
        mvc.perform(get("/plan").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 5. ownership: the user may read its own record but not someone else's
        String bearer = "Bearer " + token;
        mvc.perform(get("/user/" + saved.getId()).header("Authorization", bearer))
                .andExpect(status().isOk());
        mvc.perform(get("/user/" + java.util.UUID.randomUUID()).header("Authorization", bearer))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_withDuplicatedEmail_returnsConflict() throws Exception {
        String payload = """
                {"name":"Dup User","email":"dup-%d@example.com","password":"secret1"}
                """.formatted(System.nanoTime());

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void register_withInvalidPayload_returnsBadRequestWithFieldErrors() throws Exception {
        // blank name, malformed e-mail, password shorter than the 6-char minimum
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"not-an-email","password":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldsError").isNotEmpty());
    }
}
