package tlm.dev.lectern.diff;

final class UsageException extends RuntimeException {

    UsageException(String message) {
        super(message);
    }

    UsageException(String message, Throwable cause) {
        super(message, cause);
    }
}
