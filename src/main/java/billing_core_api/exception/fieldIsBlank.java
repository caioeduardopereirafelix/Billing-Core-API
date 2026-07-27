package billing_core_api.exception;

public class fieldIsBlank extends RuntimeException {
    public fieldIsBlank(String message) {
        super(message);
    }
}
