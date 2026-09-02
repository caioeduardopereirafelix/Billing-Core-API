package billing_core_api.exception;

/**
 * Raised when a request is well-formed and authorized but violates a domain
 * rule given the current state (e.g. cancelling an already-cancelled
 * subscription, disabling an already-inactive plan). Mapped to HTTP 409.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
