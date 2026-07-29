package billing_core_api.exception;

public class FieldIsBlank extends RuntimeException {
    public FieldIsBlank(String message) {
        super(message);
    }
}
