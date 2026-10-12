package tlm.dev.lectern.diff.write;

import java.util.List;
import tlm.dev.lectern.diff.json.CanonicalJsonWriter;
import tlm.dev.lectern.diff.json.JsonArray;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.BlockChange;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.model.CommandChange;
import tlm.dev.lectern.diff.model.LootTableChange;
import tlm.dev.lectern.diff.model.PacketChange;
import tlm.dev.lectern.diff.model.RecipeChange;
import tlm.dev.lectern.diff.model.RegistryChange;
import tlm.dev.lectern.diff.model.Skipped;
import tlm.dev.lectern.diff.model.SkippedDomain;
import tlm.dev.lectern.diff.model.SkippedFolder;
import tlm.dev.lectern.diff.model.TagChange;
import tlm.dev.lectern.diff.model.VersionRef;

public final class ChangeSetWriter {

    private final ChangeRenderers renderers;
    private final CanonicalJsonWriter json;

    public ChangeSetWriter(ChangeRenderers renderers, CanonicalJsonWriter json) {
        this.renderers = renderers;
        this.json = json;
    }

    public String write(ChangeSet changeSet) {
        List<JsonValue> changes = changeSet.changes().stream().<JsonValue>map(this::render).toList();
        JsonObject document = JsonObject.builder()
                .put("schema_version", changeSet.schemaVersion())
                .put("from", render(changeSet.from()))
                .put("to", render(changeSet.to()))
                .put("skipped", new JsonArray(changeSet.skipped().stream().<JsonValue>map(ChangeSetWriter::render).toList()))
                .put("changes", new JsonArray(changes))
                .build();
        return json.write(document);
    }

    private JsonObject render(Change change) {
        return switch (change) {
            case RegistryChange registryChange -> renderers.registries().render(registryChange);
            case BlockChange blockChange -> renderers.blocks().render(blockChange);
            case PacketChange packetChange -> renderers.packets().render(packetChange);
            case CommandChange commandChange -> renderers.commands().render(commandChange);
            case RecipeChange recipeChange -> renderers.recipes().render(recipeChange);
            case LootTableChange lootTableChange -> renderers.lootTables().render(lootTableChange);
            case TagChange tagChange -> renderers.tags().render(tagChange);
        };
    }

    private static JsonObject render(Skipped skipped) {
        return switch (skipped) {
            case SkippedDomain domain -> JsonObject.builder()
                    .put("domain", domain.domain().label())
                    .put("side", domain.side().label())
                    .put("reason", domain.reason().label())
                    .build();
            case SkippedFolder folder -> JsonObject.builder()
                    .put("folder", folder.folder())
                    .put("added", folder.added())
                    .put("removed", folder.removed())
                    .put("modified", folder.modified())
                    .put("reason", folder.reason().label())
                    .build();
        };
    }

    private static JsonObject render(VersionRef version) {
        return JsonObject.builder()
                .put("id", version.id().value())
                .put("kind", version.kind().label())
                .build();
    }
}
