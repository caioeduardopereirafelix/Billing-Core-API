package billing_core_api.exception;

public class RegistrationDuplicated extends RuntimeException {
    public RegistrationDuplicated(String message) {
        super(message);
    }
}
