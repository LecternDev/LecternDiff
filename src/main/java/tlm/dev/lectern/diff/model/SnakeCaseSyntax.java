package tlm.dev.lectern.diff.model;

import java.util.Objects;
import java.util.regex.Pattern;

final class SnakeCaseSyntax {

    private static final Pattern SHAPE = Pattern.compile("[a-z0-9_]+");

    private SnakeCaseSyntax() {
    }

    static String require(String value, String what) {
        Objects.requireNonNull(value, what);
        if (!SHAPE.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid " + what + ": '" + value + "'");
        }
        return value;
    }
}
