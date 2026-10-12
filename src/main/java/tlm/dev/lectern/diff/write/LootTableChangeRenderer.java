package tlm.dev.lectern.diff.write;

import java.util.Optional;
import tlm.dev.lectern.diff.json.JsonNull;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonString;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.LootTableAdded;
import tlm.dev.lectern.diff.model.LootTableChange;
import tlm.dev.lectern.diff.model.LootTableModified;
import tlm.dev.lectern.diff.model.LootTableRemoved;

public final class LootTableChangeRenderer {

    private final PlaceChangeRenderer places;

    public LootTableChangeRenderer(PlaceChangeRenderer places) {
        this.places = places;
    }

    public JsonObject render(LootTableChange change) {
        return switch (change) {
            case LootTableAdded added -> JsonObject.builder()
                    .put("type", "loot_table.added")
                    .put("loot_table", added.lootTable().id())
                    .put("table_type", text(added.tableType()))
                    .build();
            case LootTableRemoved removed -> JsonObject.builder()
                    .put("type", "loot_table.removed")
                    .put("loot_table", removed.lootTable().id())
                    .put("table_type", text(removed.tableType()))
                    .build();
            case LootTableModified modified -> JsonObject.builder()
                    .put("type", "loot_table.modified")
                    .put("loot_table", modified.lootTable().id())
                    .put("places", places.render(modified.places()))
                    .build();
        };
    }

    private static JsonValue text(Optional<String> value) {
        return value.<JsonValue>map(JsonString::new).orElseGet(JsonNull::new);
    }
}
