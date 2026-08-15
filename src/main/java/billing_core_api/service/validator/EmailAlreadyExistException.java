package billing_core_api.service.validator;

public class EmailAlreadyExistException extends RuntimeException {
    public EmailAlreadyExistException(String emailAlreadyExists) {
    }
}
