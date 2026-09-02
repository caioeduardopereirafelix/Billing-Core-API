package billing_core_api.exception;

/**
 * Raised when authentication fails (bad e-mail/password, disabled account, …)
 * or when a protected operation runs without an authenticated user in context.
 * Mapped to HTTP 401.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
