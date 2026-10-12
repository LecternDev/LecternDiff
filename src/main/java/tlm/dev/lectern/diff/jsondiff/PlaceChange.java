package tlm.dev.lectern.diff.jsondiff;

import java.util.Objects;
import tlm.dev.lectern.diff.json.JsonValue;

public sealed interface PlaceChange permits PlaceChange.Added, PlaceChange.Removed, PlaceChange.Modified {

    JsonPointer pointer();

    record Added(JsonPointer pointer, JsonValue value) implements PlaceChange {

        public Added {
            Objects.requireNonNull(pointer, "pointer");
            Objects.requireNonNull(value, "value");
        }
    }

    record Removed(JsonPointer pointer, JsonValue value) implements PlaceChange {

        public Removed {
            Objects.requireNonNull(pointer, "pointer");
            Objects.requireNonNull(value, "value");
        }
    }

    record Modified(JsonPointer pointer, JsonValue oldValue, JsonValue newValue) implements PlaceChange {

        public Modified {
            Objects.requireNonNull(pointer, "pointer");
            Objects.requireNonNull(oldValue, "oldValue");
            Objects.requireNonNull(newValue, "newValue");
            if (oldValue.equals(newValue)) {
                throw new IllegalArgumentException("The value at " + pointer + " did not change");
            }
        }
    }
}
