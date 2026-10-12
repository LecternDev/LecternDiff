package tlm.dev.lectern.diff.write;

import java.util.List;
import tlm.dev.lectern.diff.json.JsonArray;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.jsondiff.PlaceChange;

public final class PlaceChangeRenderer {

    public JsonValue render(List<PlaceChange> places) {
        return new JsonArray(places.stream().<JsonValue>map(this::render).toList());
    }

    private JsonObject render(PlaceChange place) {
        return switch (place) {
            case PlaceChange.Added added -> JsonObject.builder()
                    .put("operation", "added")
                    .put("pointer", added.pointer().text())
                    .put("new_value", added.value())
                    .build();
            case PlaceChange.Removed removed -> JsonObject.builder()
                    .put("operation", "removed")
                    .put("pointer", removed.pointer().text())
                    .put("old_value", removed.value())
                    .build();
            case PlaceChange.Modified modified -> JsonObject.builder()
                    .put("operation", "modified")
                    .put("pointer", modified.pointer().text())
                    .put("old_value", modified.oldValue())
                    .put("new_value", modified.newValue())
                    .build();
        };
    }
}
