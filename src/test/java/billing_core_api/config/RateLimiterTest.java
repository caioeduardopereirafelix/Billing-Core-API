package billing_core_api.config;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    @Test
    void allowsUpToTheConfiguredLimitWithinTheWindow() {
        RateLimiter limiter = new RateLimiter(3, 60, Instant::now);

        assertTrue(limiter.tryAcquire("key"));
        assertTrue(limiter.tryAcquire("key"));
        assertTrue(limiter.tryAcquire("key"));
    }

    @Test
    void rejectsOnceTheLimitIsExceeded() {
        RateLimiter limiter = new RateLimiter(2, 60, Instant::now);

        assertTrue(limiter.tryAcquire("key"));
        assertTrue(limiter.tryAcquire("key"));
        assertFalse(limiter.tryAcquire("key"));
    }

    @Test
    void tracksDifferentKeysIndependently() {
        RateLimiter limiter = new RateLimiter(1, 60, Instant::now);

        assertTrue(limiter.tryAcquire("alice"));
        assertTrue(limiter.tryAcquire("bob"));
        assertFalse(limiter.tryAcquire("alice"));
        assertFalse(limiter.tryAcquire("bob"));
    }

    @Test
    void resetsOnceTheWindowExpires() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2024-01-01T00:00:00Z"));
        RateLimiter limiter = new RateLimiter(1, 60, now::get);

        assertTrue(limiter.tryAcquire("key"));
        assertFalse(limiter.tryAcquire("key"));

        now.set(now.get().plusSeconds(61));

        assertTrue(limiter.tryAcquire("key"));
    }
}
