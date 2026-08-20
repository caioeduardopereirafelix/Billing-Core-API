package billing_core_api.config;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

    class TokenProviderTest {

        private TokenProvider provider;

        private static final String SECRET =
                "g0VI2r1e0K6izUZjygj0RtW1DQPIq3HGX9aqDFMyZBppS8mNL03G5Ldb7HMX38JZnfFsLRaGuv4R7pD2L2gE6n";

        @BeforeEach
        void setUp() {
            provider = new TokenProvider();
            ReflectionTestUtils.setField(provider, "secret", SECRET);
            ReflectionTestUtils.setField(provider, "expirationTime", 3600000L);
        }

        @Test
        void shouldGenerateValidToken() {
            User user = (User) User.withUsername("caio@email.com")
                    .password("password")
                    .authorities("ROLE_USER")
                    .build();

            String token = provider.generaToker(
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            user.getAuthorities()
                    )
            );

            assertNotNull(token);
            assertTrue(provider.isTokenValid(token));
            assertEquals("caio@email.com", provider.getUsername(token));
        }

        @Test
        void shouldRejectMalformedToken() {
            assertFalse(provider.isTokenValid("not-a-jwt"));
        }

        @Test
        void shouldRejectTokenSignedWithAnotherKey() {
            String otherSecret =
                    "9h8G3r5L2w7Q1e6K4m9P2s5V8x1Z6c3B7n4M8q2W5r7T1y6U9i3O6p4A8s2D5f7";
            String token = Jwts.builder()
                    .subject("caio@email.com")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 3600000))
                    .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                            otherSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                    .compact();

            assertFalse(provider.isTokenValid(token));
        }

        @Test
        void shouldRejectExpiredToken() {
            String token = Jwts.builder()
                    .subject("caio@email.com")
                    .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                    .expiration(new Date(System.currentTimeMillis() - 3600000))
                    .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                            SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                    .compact();

            assertFalse(provider.isTokenValid(token));
        }

        @Test
        void shouldExtractUsernameFromValidToken() {
            User user = (User) User.withUsername("caio@email.com")
                    .password("password")
                    .authorities("ROLE_USER")
                    .build();

            String token = provider.generaToker(
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
            );

            assertEquals("caio@email.com", provider.getUsername(token));
        }
    }

