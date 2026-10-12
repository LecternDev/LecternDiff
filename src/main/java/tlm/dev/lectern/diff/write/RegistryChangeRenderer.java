package tlm.dev.lectern.diff.write;

import java.util.Optional;
import tlm.dev.lectern.diff.json.JsonNull;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonString;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.EntryKey;
import tlm.dev.lectern.diff.model.RegistryAdded;
import tlm.dev.lectern.diff.model.RegistryChange;
import tlm.dev.lectern.diff.model.RegistryDefaultModified;
import tlm.dev.lectern.diff.model.RegistryEntryAdded;
import tlm.dev.lectern.diff.model.RegistryEntryIdModified;
import tlm.dev.lectern.diff.model.RegistryEntryIdsShifted;
import tlm.dev.lectern.diff.model.RegistryEntryRemoved;
import tlm.dev.lectern.diff.model.RegistryIdModified;
import tlm.dev.lectern.diff.model.RegistryIdsShifted;
import tlm.dev.lectern.diff.model.RegistryRemoved;

public final class RegistryChangeRenderer {

    public JsonObject render(RegistryChange change) {
        return switch (change) {
            case RegistryAdded added -> JsonObject.builder()
                    .put("type", "registry.added")
                    .put("registry", added.registry().value())
                    .put("protocol_id", added.id().value())
                    .put("entry_count", added.entryCount())
                    .build();
            case RegistryRemoved removed -> JsonObject.builder()
                    .put("type", "registry.removed")
                    .put("registry", removed.registry().value())
                    .put("protocol_id", removed.id().value())
                    .put("entry_count", removed.entryCount())
                    .build();
            case RegistryDefaultModified modified -> JsonObject.builder()
                    .put("type", "registry.default_modified")
                    .put("registry", modified.registry().value())
                    .put("old_default", optionalEntry(modified.oldDefault()))
                    .put("new_default", optionalEntry(modified.newDefault()))
                    .build();
            case RegistryIdModified modified -> JsonObject.builder()
                    .put("type", "registry.id_modified")
                    .put("registry", modified.registry().value())
                    .put("old_id", modified.oldId().value())
                    .put("new_id", modified.newId().value())
                    .build();
            case RegistryIdsShifted shifted -> JsonObject.builder()
                    .put("type", "registry.ids_shifted")
                    .put("count", shifted.count())
                    .put("first_id", shifted.firstId().value())
                    .build();
            case RegistryEntryAdded added -> JsonObject.builder()
                    .put("type", "registry.entry_added")
                    .put("registry", added.registry().value())
                    .put("entry", added.entry().value())
                    .put("protocol_id", added.id().value())
                    .build();
            case RegistryEntryRemoved removed -> JsonObject.builder()
                    .put("type", "registry.entry_removed")
                    .put("registry", removed.registry().value())
                    .put("entry", removed.entry().value())
                    .put("protocol_id", removed.id().value())
                    .build();
            case RegistryEntryIdModified modified -> JsonObject.builder()
                    .put("type", "registry.entry_id_modified")
                    .put("registry", modified.registry().value())
                    .put("entry", modified.entry().value())
                    .put("old_id", modified.oldId().value())
                    .put("new_id", modified.newId().value())
                    .build();
            case RegistryEntryIdsShifted shifted -> JsonObject.builder()
                    .put("type", "registry.entry_ids_shifted")
                    .put("registry", shifted.registry().value())
                    .put("count", shifted.count())
                    .put("first_id", shifted.firstId().value())
                    .build();
        };
    }

    private static JsonValue optionalEntry(Optional<EntryKey> entry) {
        return entry.<JsonValue>map(key -> new JsonString(key.value())).orElseGet(JsonNull::new);
    }
}
