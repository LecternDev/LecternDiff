package tlm.dev.lectern.diff.model;

import java.util.Objects;
import java.util.Optional;

public record LootTableAdded(DataResource lootTable, Optional<String> tableType) implements LootTableChange {

    public LootTableAdded {
        Objects.requireNonNull(lootTable, "lootTable");
        if (!lootTable.kind().equals(ResourceKind.LOOT_TABLE)) {
            throw new IllegalArgumentException("Not a loot table: " + lootTable);
        }
        Objects.requireNonNull(tableType, "tableType");
        tableType.ifPresent(value -> NamespacedName.require(value, "loot table type"));
    }
}
