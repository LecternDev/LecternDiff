package tlm.dev.lectern.diff.read;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import tlm.dev.lectern.diff.json.JsonArray;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.json.JsonString;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.DataFiles;
import tlm.dev.lectern.diff.model.DataResource;
import tlm.dev.lectern.diff.model.LootTable;
import tlm.dev.lectern.diff.model.Recipe;
import tlm.dev.lectern.diff.model.ResourceKind;
import tlm.dev.lectern.diff.model.Tag;

public final class DataReader {

    public static final String ROOT = "data";

    private static final String TYPE = "type";
    private static final String REPLACE = "replace";
    private static final String VALUES = "values";
    private static final Set<String> TAG_KEYS = Set.of(REPLACE, VALUES);
    private static final List<String> RESULT_KEYS = List.of("result", "output");
    private static final List<String> ITEM_KEYS = List.of("id", "item");

    private final JsonReader json;
    private final DataResourceLocator locator = new DataResourceLocator(new SingularFolderNames());

    private interface Interpreter<T> {

        T interpret(ReportContext file, JsonValue content);
    }

    public DataReader(JsonReader json) {
        this.json = json;
    }

    public Optional<DataFiles<Recipe>> recipes(VersionTree tree) {
        return read(tree, ResourceKind.RECIPE.folder()::equals, this::recipe);
    }

    public Optional<DataFiles<LootTable>> lootTables(VersionTree tree) {
        return read(tree, ResourceKind.LOOT_TABLE.folder()::equals, this::lootTable);
    }

    public Optional<DataFiles<Tag>> tags(VersionTree tree) {
        return read(tree, ResourceKind.TAGS_FOLDER::equals, this::tag);
    }

    public Optional<DataFiles<JsonValue>> otherFolders(VersionTree tree, Set<String> excludedFolders) {
        return read(tree, folder -> !excludedFolders.contains(folder), (file, content) -> content);
    }

    private <T> Optional<DataFiles<T>> read(VersionTree tree, Predicate<String> wantedFolder, Interpreter<T> interpreter) {
        List<String> paths = tree.files(ROOT);
        if (paths.isEmpty()) {
            return Optional.empty();
        }
        Map<DataResource, T> found = new TreeMap<>();
        for (String path : paths) {
            ReportContext file = new ReportContext(tree.version().id(), path);
            DataResource resource = locator.locate(path, file);
            if (!wantedFolder.test(resource.kind().folder())) {
                continue;
            }
            JsonValue content = file.parse(tree, json).orElseThrow(() -> file.failure("the file disappeared while reading"));
            if (found.putIfAbsent(resource, interpreter.interpret(file, content)) != null) {
                throw file.failure("another file gives the same resource " + resource);
            }
        }
        return Optional.of(new DataFiles<>(found));
    }

    private Recipe recipe(ReportContext file, JsonValue content) {
        JsonObject object = file.object(content, "the recipe");
        String type = file.string(file.member(object, TYPE, "the recipe"), "type of the recipe");
        try {
            return new Recipe(type, resultOf(object), content);
        } catch (IllegalArgumentException e) {
            throw file.failure(e.getMessage(), e);
        }
    }

    private LootTable lootTable(ReportContext file, JsonValue content) {
        JsonObject object = file.object(content, "the loot table");
        Optional<String> type = object.member(TYPE).map(value -> file.string(value, "type of the loot table"));
        try {
            return new LootTable(type, content);
        } catch (IllegalArgumentException e) {
            throw file.failure(e.getMessage(), e);
        }
    }

    private Tag tag(ReportContext file, JsonValue content) {
        JsonObject object = file.object(content, "the tag");
        file.rejectUnknownKeys(object, TAG_KEYS, "the tag");
        boolean replace = object.member(REPLACE).map(value -> file.bool(value, "replace of the tag")).orElse(false);
        JsonArray values = file.array(file.member(object, VALUES, "the tag"), "values of the tag");
        Set<String> distinct = new TreeSet<>();
        for (JsonValue value : values.elements()) {
            distinct.add(file.string(value, "a value of the tag"));
        }
        try {
            return new Tag(replace, distinct);
        } catch (IllegalArgumentException e) {
            throw file.failure(e.getMessage(), e);
        }
    }

    private static Optional<String> resultOf(JsonObject recipe) {
        for (String key : RESULT_KEYS) {
            Optional<JsonValue> value = recipe.member(key);
            if (value.isPresent()) {
                return itemIdOf(value.get());
            }
        }
        return Optional.empty();
    }

    private static Optional<String> itemIdOf(JsonValue value) {
        if (value instanceof JsonString text) {
            return Optional.of(text.value());
        }
        if (value instanceof JsonObject object) {
            List<String> candidates = new ArrayList<>();
            for (String key : ITEM_KEYS) {
                object.member(key).filter(JsonString.class::isInstance).map(JsonString.class::cast).ifPresent(text -> candidates.add(text.value()));
            }
            return candidates.stream().findFirst();
        }
        return Optional.empty();
    }
}
