package billing_core_api.exception;

public class PlanAlreadyExists extends RuntimeException {
    public PlanAlreadyExists(String message) {
        super(message);
    }
}
