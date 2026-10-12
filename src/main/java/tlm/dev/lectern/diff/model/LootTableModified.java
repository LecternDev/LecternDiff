package tlm.dev.lectern.diff.model;

import java.util.List;
import java.util.Objects;
import tlm.dev.lectern.diff.jsondiff.PlaceChange;

public record LootTableModified(DataResource lootTable, List<PlaceChange> places) implements LootTableChange {

    public LootTableModified {
        Objects.requireNonNull(lootTable, "lootTable");
        if (!lootTable.kind().equals(ResourceKind.LOOT_TABLE)) {
            throw new IllegalArgumentException("Not a loot table: " + lootTable);
        }
        places = List.copyOf(places);
        if (places.isEmpty()) {
            throw new IllegalArgumentException("The loot table " + lootTable + " has no changed place");
        }
    }
}
