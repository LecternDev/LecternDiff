package tlm.dev.lectern.diff.model;

import java.util.Objects;
import java.util.Optional;
import tlm.dev.lectern.diff.json.JsonValue;

public record LootTable(Optional<String> type, JsonValue content) {

    public LootTable {
        Objects.requireNonNull(type, "type");
        type.ifPresent(value -> NamespacedName.require(value, "loot table type"));
        Objects.requireNonNull(content, "content");
    }
}
