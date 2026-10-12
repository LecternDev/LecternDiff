package tlm.dev.lectern.diff.jsondiff;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import tlm.dev.lectern.diff.json.JsonArray;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonValue;

public final class JsonComparer {

    public List<PlaceChange> compare(JsonValue from, JsonValue to) {
        List<PlaceChange> places = new ArrayList<>();
        compare(JsonPointer.root(), from, to, places);
        return List.copyOf(places);
    }

    private void compare(JsonPointer pointer, JsonValue from, JsonValue to, List<PlaceChange> places) {
        if (from.equals(to)) {
            return;
        }
        switch (from) {
            case JsonObject before when to instanceof JsonObject after -> compareObjects(pointer, before, after, places);
            case JsonArray before when to instanceof JsonArray after -> compareArrays(pointer, before, after, places);
            default -> places.add(new PlaceChange.Modified(pointer, from, to));
        }
    }

    private void compareObjects(JsonPointer pointer, JsonObject from, JsonObject to, List<PlaceChange> places) {
        Map<String, JsonValue> before = from.members();
        Map<String, JsonValue> after = to.members();
        TreeSet<String> names = new TreeSet<>(before.keySet());
        names.addAll(after.keySet());
        for (String name : names) {
            JsonPointer child = pointer.child(name);
            JsonValue was = before.get(name);
            JsonValue now = after.get(name);
            if (was == null) {
                places.add(new PlaceChange.Added(child, now));
            } else if (now == null) {
                places.add(new PlaceChange.Removed(child, was));
            } else {
                compare(child, was, now, places);
            }
        }
    }

    private void compareArrays(JsonPointer pointer, JsonArray from, JsonArray to, List<PlaceChange> places) {
        List<JsonValue> before = from.elements();
        List<JsonValue> after = to.elements();
        int shared = Math.min(before.size(), after.size());
        for (int index = 0; index < shared; index++) {
            compare(pointer.child(index), before.get(index), after.get(index), places);
        }
        for (int index = shared; index < before.size(); index++) {
            places.add(new PlaceChange.Removed(pointer.child(index), before.get(index)));
        }
        for (int index = shared; index < after.size(); index++) {
            places.add(new PlaceChange.Added(pointer.child(index), after.get(index)));
        }
    }
}
