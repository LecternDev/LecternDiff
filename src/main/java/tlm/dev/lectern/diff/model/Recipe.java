package tlm.dev.lectern.diff.model;

import java.util.Objects;
import java.util.Optional;
import tlm.dev.lectern.diff.json.JsonValue;

public record Recipe(String type, Optional<String> result, JsonValue content) {

    public Recipe {
        NamespacedName.require(type, "recipe type");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(content, "content");
    }
}
