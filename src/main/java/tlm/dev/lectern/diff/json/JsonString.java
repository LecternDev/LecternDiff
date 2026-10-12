package tlm.dev.lectern.diff.json;

import java.util.Objects;

public record JsonString(String value) implements JsonValue {

    public JsonString {
        Objects.requireNonNull(value, "value");
    }
}
