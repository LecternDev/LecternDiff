package tlm.dev.lectern.diff.write;

import java.util.List;
import tlm.dev.lectern.diff.json.CanonicalJsonWriter;
import tlm.dev.lectern.diff.json.JsonArray;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.model.RegistryChange;
import tlm.dev.lectern.diff.model.VersionRef;

public final class ChangeSetWriter {

    private final RegistryChangeRenderer registryRenderer;
    private final CanonicalJsonWriter json;

    public ChangeSetWriter(RegistryChangeRenderer registryRenderer, CanonicalJsonWriter json) {
        this.registryRenderer = registryRenderer;
        this.json = json;
    }

    public String write(ChangeSet changeSet) {
        List<JsonValue> changes = changeSet.changes().stream().<JsonValue>map(this::render).toList();
        JsonObject document = JsonObject.builder()
                .put("schema_version", changeSet.schemaVersion())
                .put("from", render(changeSet.from()))
                .put("to", render(changeSet.to()))
                .put("changes", new JsonArray(changes))
                .build();
        return json.write(document);
    }

    private JsonObject render(Change change) {
        return switch (change) {
            case RegistryChange registryChange -> registryRenderer.render(registryChange);
        };
    }

    private static JsonObject render(VersionRef version) {
        return JsonObject.builder()
                .put("id", version.id().value())
                .put("kind", version.kind().label())
                .build();
    }
}
