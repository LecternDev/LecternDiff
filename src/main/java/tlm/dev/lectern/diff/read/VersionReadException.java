package tlm.dev.lectern.diff.read;

public final class VersionReadException extends RuntimeException {

    public VersionReadException(String message) {
        super(message);
    }

    public VersionReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
