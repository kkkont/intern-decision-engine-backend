package ee.taltech.inbankbackend.exceptions;

public class NotValidAgeException extends Throwable {
    private final String message;
    private final Throwable cause;

    public NotValidAgeException(String message) {
        this(message, null);
    }

    public NotValidAgeException(String message, Throwable cause) {
        this.message = message;
        this.cause = cause;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
