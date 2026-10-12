package tlm.dev.lectern.diff.json;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public record JsonObject(Map<String, JsonValue> members) implements JsonValue {

    public JsonObject {
        members = Collections.unmodifiableMap(new TreeMap<>(members));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<JsonValue> member(String name) {
        return Optional.ofNullable(members.get(name));
    }

    public static final class Builder {

        private final Map<String, JsonValue> members = new TreeMap<>();

        private Builder() {
        }

        public Builder put(String name, JsonValue value) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(value, "value");
            if (members.putIfAbsent(name, value) != null) {
                throw new IllegalArgumentException("Member '" + name + "' is already set");
            }
            return this;
        }

        public Builder put(String name, String value) {
            return put(name, new JsonString(value));
        }

        public Builder put(String name, long value) {
            return put(name, JsonNumber.of(value));
        }

        public Builder putNull(String name) {
            return put(name, new JsonNull());
        }

        public JsonObject build() {
            return new JsonObject(members);
        }
    }
}
