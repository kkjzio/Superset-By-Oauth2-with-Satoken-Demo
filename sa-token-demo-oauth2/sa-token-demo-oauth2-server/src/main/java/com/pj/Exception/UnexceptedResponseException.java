package superset.client.exception;

import java.rmi.ConnectException;

import lombok.Getter;

public class UnexceptedResponseException extends ConnectException {
    @Getter
    private int code;
    @Getter
    private String endpoint;

    public UnexceptedResponseException(String endpoint, int code, String message) {
        this(String.format("endpoint=%s, code=%d, message=%s",endpoint ,code, message));
        this.code = code;
        this.endpoint = endpoint;
    }

    private UnexceptedResponseException(String message) {
        super(message);
    }
}
