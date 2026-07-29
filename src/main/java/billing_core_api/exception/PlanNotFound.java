package billing_core_api.exception;

public class PlanNotFound extends RuntimeException {
    public PlanNotFound(String message) {
        super(message);
    }
}
