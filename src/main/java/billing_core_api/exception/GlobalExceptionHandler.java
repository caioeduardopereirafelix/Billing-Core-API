package billing_core_api.exception;

import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(FieldIsBlank.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseError handleFieldIsBlank(FieldIsBlank e) {
        return new ResponseError(HttpStatus.BAD_REQUEST.value(), e.getMessage(), List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseError handleUnreadableBody(HttpMessageNotReadableException e) {
        return new ResponseError(HttpStatus.BAD_REQUEST.value(), "Malformed or missing request body", List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseError handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "Parameter '" + e.getName() + "' has an invalid value: " + e.getValue();
        return new ResponseError(
                HttpStatus.BAD_REQUEST.value(),
                message,
                List.of(new ErrorField(e.getName(), "invalid value"))
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseError handleIllegalArgument(IllegalArgumentException e) {
        return new ResponseError(HttpStatus.BAD_REQUEST.value(), e.getMessage(), List.of());
    }


    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseError handleInvalidCredentials(InvalidCredentialsException e) {
        return new ResponseError(HttpStatus.UNAUTHORIZED.value(), e.getMessage(), List.of());
    }


    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public ResponseError handleInsufficientBalance(InsufficientBalanceException e) {
        return new ResponseError(HttpStatus.PAYMENT_REQUIRED.value(), e.getMessage(), List.of());
    }



    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseError handleAccessDenied(AccessDeniedException e) {
        return new ResponseError(HttpStatus.FORBIDDEN.value(), e.getMessage(), List.of());
    }



    @ExceptionHandler({PlanNotFound.class, SubscriptionNotFoundException.class, UserNotFound.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseError handleNotFound(RuntimeException e) {
        return new ResponseError(HttpStatus.NOT_FOUND.value(), e.getMessage(), List.of());
    }

    @ExceptionHandler({
            EmailAlreadyExistException.class,
            RegistrationDuplicated.class,
            PlanAlreadyExists.class,
            BusinessRuleException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseError handleConflict(RuntimeException e) {
        return new ResponseError(HttpStatus.CONFLICT.value(), e.getMessage(), List.of());
    }



    @ExceptionHandler(AmqpException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ResponseError handleMessagingDown(AmqpException e) {
        return new ResponseError(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Messaging broker is unavailable, please retry later",
                List.of()
        );
    }
}
