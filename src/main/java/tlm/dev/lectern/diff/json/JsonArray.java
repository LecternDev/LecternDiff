package tlm.dev.lectern.diff.json;

import java.util.List;

public record JsonArray(List<JsonValue> elements) implements JsonValue {

    public JsonArray {
        elements = List.copyOf(elements);
    }
}
