package tlm.dev.lectern.diff.read;

import java.util.Map;

final class SingularFolderNames {

    private static final Map<String, String> SINGULAR = Map.of(
            "advancements", "advancement",
            "blocks", "block",
            "entity_types", "entity_type",
            "fluids", "fluid",
            "game_events", "game_event",
            "items", "item",
            "loot_tables", "loot_table",
            "recipes", "recipe");

    String singular(String folder) {
        return SINGULAR.getOrDefault(folder, folder);
    }
}
