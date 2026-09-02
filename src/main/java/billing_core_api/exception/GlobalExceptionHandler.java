package billing_core_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseError handleValidation(MethodArgumentNotValidException e) {
        List<ErrorField> fields = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorField(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return new ResponseError(HttpStatus.BAD_REQUEST.value(), "Validation failed", fields);
    }

    @ExceptionHandler(InvalidFieldException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseError handleInvalidField(InvalidFieldException e) {
        return new ResponseError(
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                List.of(new ErrorField(e.getCampo(), e.getMessage()))
        );
    }

    @ExceptionHandler({EmailAlreadyExistException.class, RegistrationDuplicated.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseError handleDuplicated(RuntimeException e) {
        return new ResponseError(HttpStatus.CONFLICT.value(), e.getMessage(), List.of());
    }
}
