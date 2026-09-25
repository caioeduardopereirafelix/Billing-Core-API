package billing_core_api.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;


@Component
public class RateLimiter {

    private final int maxRequests;
    private final long windowSeconds;
    private final Supplier<Instant> clock;
    private final Map<String, Window> buckets = new ConcurrentHashMap<>();

    @Autowired
    public RateLimiter(@Value("${rate-limit.auth.max-requests:10}") int maxRequests,
                        @Value("${rate-limit.auth.window-seconds:60}") long windowSeconds) {
        this(maxRequests, windowSeconds, Instant::now);
    }

    RateLimiter(int maxRequests, long windowSeconds, Supplier<Instant> clock) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        this.clock = clock;
    }

    public boolean tryAcquire(String key) {
        Instant now = clock.get();

        Window window = buckets.compute(key, (k, existing) -> {
            if (existing == null || now.isAfter(existing.expiresAt)) {
                return new Window(now.plusSeconds(windowSeconds), new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        return window.count.get() <= maxRequests;
    }

    private static final class Window {
        private final Instant expiresAt;
        private final AtomicInteger count;

        private Window(Instant expiresAt, AtomicInteger count) {
            this.expiresAt = expiresAt;
            this.count = count;
        }
    }
}
