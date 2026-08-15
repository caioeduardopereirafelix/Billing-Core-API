package billing_core_api.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public record ResponseError(int status, String error, List<ErrorField> fieldsError) {

    public static ResponseError errorStandard(String msg){
        return new ResponseError(HttpStatus.BAD_REQUEST.value(), msg, List.of());
    }
}