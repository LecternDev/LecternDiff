package tlm.dev.lectern.diff.jsondiff;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record JsonPointer(List<String> tokens) {

    private static final JsonPointer ROOT = new JsonPointer(List.of());

    public JsonPointer {
        tokens = List.copyOf(tokens);
    }

    public static JsonPointer root() {
        return ROOT;
    }

    public JsonPointer child(String token) {
        Objects.requireNonNull(token, "token");
        List<String> longer = new ArrayList<>(tokens);
        longer.add(token);
        return new JsonPointer(longer);
    }

    public JsonPointer child(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("An array index is not negative: " + index);
        }
        return child(Integer.toString(index));
    }

    public String text() {
        StringBuilder text = new StringBuilder();
        for (String token : tokens) {
            text.append('/').append(token.replace("~", "~0").replace("/", "~1"));
        }
        return text.toString();
    }

    @Override
    public String toString() {
        return text();
    }
}
