package billing_core_api.service.validator;

public class RegistrationDuplicated extends RuntimeException {
    public RegistrationDuplicated(String message) {
        super(message);
    }
}
