package billing_core_api.exception;

public class InvalidFieldException extends RuntimeException {
    private String campo;

    public InvalidFieldException(String campo, String msg){
        super(msg);
        this.campo = campo;
    }
}
