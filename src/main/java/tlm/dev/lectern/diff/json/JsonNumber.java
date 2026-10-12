package tlm.dev.lectern.diff.json;

import java.util.Objects;
import java.util.regex.Pattern;

public record JsonNumber(String text) implements JsonValue {

    private static final Pattern SHAPE = Pattern.compile("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?");

    public JsonNumber {
        Objects.requireNonNull(text, "text");
        if (!SHAPE.matcher(text).matches()) {
            throw new IllegalArgumentException("Not a JSON number: '" + text + "'");
        }
    }

    public static JsonNumber of(long value) {
        return new JsonNumber(Long.toString(value));
    }
}
