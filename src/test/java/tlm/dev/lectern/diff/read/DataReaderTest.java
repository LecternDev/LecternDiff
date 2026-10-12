package tlm.dev.lectern.diff.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.json;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.lootTable;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.recipe;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.resource;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.tag;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.fixtures.InMemoryVersionTree;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.DataFiles;
import tlm.dev.lectern.diff.model.DataResource;
import tlm.dev.lectern.diff.model.LootTable;
import tlm.dev.lectern.diff.model.Recipe;
import tlm.dev.lectern.diff.model.Tag;

class DataReaderTest {

    private static final String PLAIN_RECIPE = "{\"type\": \"demo:smelting\", \"result\": \"demo:ingot\"}";

    private final DataReader reader = new DataReader(new JsonReader());

    private static VersionTree tree(Map<String, String> files) {
        return new InMemoryVersionTree(release("9.1"), files);
    }

    private static VersionTree tree(String path, String content) {
        return tree(Map.of(path, content));
    }

    private static void assertFailure(String path, String fragment, Runnable read) {
        VersionReadException failure = assertThrows(VersionReadException.class, read::run);
        assertTrue(failure.getMessage().startsWith("Version 9.1, " + path + ": "), failure.getMessage());
        assertTrue(failure.getMessage().contains(fragment), failure.getMessage());
    }

    @Test
    void aVersionWithoutADataFolderGivesNothingForEveryKind() {
        VersionTree empty = tree(Map.of("reports/blocks.json", "{}"));
        assertEquals(Optional.empty(), reader.recipes(empty));
        assertEquals(Optional.empty(), reader.lootTables(empty));
        assertEquals(Optional.empty(), reader.tags(empty));
        assertEquals(Optional.empty(), reader.otherFolders(empty, Set.of()));
    }

    @Test
    void aResourceIsItsNamespaceItsKindAndItsNameWithoutTheJsonSuffix() {
        DataFiles<Recipe> recipes = reader.recipes(tree(Map.of(
                "data/demo/recipe/acorn.json", PLAIN_RECIPE, "data/demo/recipe/brewing/potion.json", PLAIN_RECIPE,
                "data/other/recipe/acorn.json", PLAIN_RECIPE))).orElseThrow();
        assertEquals(List.of(resource("demo", "recipe", "acorn"), resource("demo", "recipe", "brewing/potion"),
                resource("other", "recipe", "acorn")), List.copyOf(recipes.files().keySet()));
    }

    @Test
    void aPluralFolderGivesTheSameResourceAsTheSingularOne() {
        DataFiles<Recipe> before = reader.recipes(tree("data/demo/recipes/acorn.json", PLAIN_RECIPE)).orElseThrow();
        DataFiles<Recipe> after = reader.recipes(tree("data/demo/recipe/acorn.json", PLAIN_RECIPE)).orElseThrow();
        assertEquals(before, after);
        assertEquals(List.of(recipe("acorn")), List.copyOf(after.files().keySet()));
        DataFiles<LootTable> tables = reader.lootTables(tree("data/demo/loot_tables/blocks/crate.json", "{\"type\": \"demo:block\"}"))
                .orElseThrow();
        assertEquals(List.of(lootTable("blocks/crate")), List.copyOf(tables.files().keySet()));
    }

    @Test
    void tagsAreBelowTheirRegistryAndAPluralRegistryIsSingularized() {
        String values = "{\"values\": [\"demo:a\"]}";
        DataFiles<Tag> tags = reader.tags(tree(Map.of(
                "data/demo/tags/blocks/logs.json", values, "data/demo/tags/block/mineable/axe.json", values,
                "data/demo/tags/entity_types/boss.json", values, "data/demo/tags/worldgen/biome/cold.json", values))).orElseThrow();
        assertEquals(List.of(tag("block", "logs"), tag("block", "mineable/axe"), tag("entity_type", "boss"), tag("worldgen/biome", "cold")),
                List.copyOf(tags.files().keySet()));
    }

    @Test
    void aMcmetaFileKeepsItsFullNameAndIsNotARecipeOrLootTableOrTag() {
        VersionTree pack = tree(Map.of("data/demo/datapacks/trade_pack/pack.mcmeta", "{\"pack\": {}}",
                "data/demo/datapacks/trade_pack/data/demo/recipe/x.json", PLAIN_RECIPE));
        assertEquals(List.of(resource("demo", "datapacks", "trade_pack/data/demo/recipe/x"),
                resource("demo", "datapacks", "trade_pack/pack.mcmeta")), List.copyOf(reader.otherFolders(pack, Set.of()).orElseThrow().files().keySet()));
        assertEquals(List.of(), List.copyOf(reader.recipes(pack).orElseThrow().files().keySet()));
        assertEquals(List.of(), List.copyOf(reader.tags(pack).orElseThrow().files().keySet()));
    }

    @Test
    void otherFoldersAreEveryFolderThatIsNotExcluded() {
        VersionTree tree = tree(Map.of(
                "data/demo/recipe/a.json", PLAIN_RECIPE, "data/demo/loot_table/b.json", "{}", "data/demo/tags/block/c.json", "{\"values\": []}",
                "data/demo/worldgen/biome/d.json", "{\"x\": 1}", "data/demo/advancements/e.json", "[]"));
        DataFiles<JsonValue> others = reader.otherFolders(tree, Set.of("recipe", "loot_table", "tags")).orElseThrow();
        assertEquals(List.of(resource("demo", "advancement", "e"), resource("demo", "worldgen", "biome/d")),
                List.copyOf(others.files().keySet()));
        assertEquals(json("[]"), others.files().get(resource("demo", "advancement", "e")));
    }

    @Test
    void aRecipeKeepsItsTypeItsContentAndAResultWhateverTheGeneration() {
        Map<String, String> files = Map.of(
                "data/demo/recipe/string.json", "{\"type\": \"demo:smelting\", \"result\": \"demo:a\"}",
                "data/demo/recipe/id.json", "{\"type\": \"demo:crafting\", \"result\": {\"id\": \"demo:b\", \"count\": 2}}",
                "data/demo/recipe/item.json", "{\"type\": \"demo:crafting\", \"result\": {\"item\": \"demo:c\"}}",
                "data/demo/recipe/output.json", "{\"type\": \"demo:brewing\", \"output\": {\"id\": \"demo:d\", \"components\": {}}}",
                "data/demo/recipe/empty.json", "{\"type\": \"demo:transmute\", \"result\": {}}",
                "data/demo/recipe/none.json", "{\"type\": \"demo:special\"}",
                "data/demo/recipe/number.json", "{\"type\": \"demo:odd\", \"result\": 3}");
        Map<DataResource, Recipe> recipes = reader.recipes(tree(files)).orElseThrow().files();
        assertEquals(Optional.of("demo:a"), recipes.get(recipe("string")).result());
        assertEquals(Optional.of("demo:b"), recipes.get(recipe("id")).result());
        assertEquals(Optional.of("demo:c"), recipes.get(recipe("item")).result());
        assertEquals(Optional.of("demo:d"), recipes.get(recipe("output")).result());
        assertEquals(Optional.empty(), recipes.get(recipe("empty")).result());
        assertEquals(Optional.empty(), recipes.get(recipe("none")).result());
        assertEquals(Optional.empty(), recipes.get(recipe("number")).result());
        assertEquals("demo:brewing", recipes.get(recipe("output")).type());
        assertEquals(json(files.get("data/demo/recipe/id.json")), recipes.get(recipe("id")).content());
    }

    @Test
    void aLootTableTypeIsOptional() {
        Map<DataResource, LootTable> tables = reader.lootTables(tree(Map.of(
                "data/demo/loot_table/typed.json", "{\"type\": \"demo:chest\", \"pools\": []}",
                "data/demo/loot_table/plain.json", "{\"pools\": []}"))).orElseThrow().files();
        assertEquals(Optional.of("demo:chest"), tables.get(lootTable("typed")).type());
        assertEquals(Optional.empty(), tables.get(lootTable("plain")).type());
    }

    @Test
    void aTagKeepsDistinctValuesAndReplaceDefaultsToFalse() {
        Map<DataResource, Tag> tags = reader.tags(tree(Map.of(
                "data/demo/tags/block/old.json", "{\"replace\": true, \"values\": [\"demo:b\", \"#demo:a\", \"demo:b\"]}",
                "data/demo/tags/block/new.json", "{\"values\": []}"))).orElseThrow().files();
        assertEquals(new Tag(true, Set.of("demo:b", "#demo:a")), tags.get(tag("block", "old")));
        assertEquals(new Tag(false, Set.of()), tags.get(tag("block", "new")));
    }

    @Test
    void invalidJsonIsRefusedWithTheVersionAndTheFileAsWritten() {
        String path = "data/demo/recipes/broken.json";
        assertFailure(path, "not valid JSON", () -> reader.recipes(tree(path, "{\"type\": ")));
    }

    @Test
    void aFileThatIsNotJsonOrMcmetaIsRefusedWhateverTheFolderTheCallerWants() {
        String path = "data/demo/structure/castle.nbt";
        assertFailure(path, "unexpected file", () -> reader.recipes(tree(path, "x")));
    }

    @Test
    void aFileOutsideAResourceFolderOfANamespaceIsRefused() {
        assertFailure("data/demo/loose.json", "not inside a resource folder", () -> reader.otherFolders(tree("data/demo/loose.json", "{}"), Set.of()));
        assertFailure("data/loose.json", "not inside a resource folder", () -> reader.otherFolders(tree("data/loose.json", "{}"), Set.of()));
    }

    @Test
    void aTagDirectlyUnderTagsOrUnderWorldgenIsRefused() {
        assertFailure("data/demo/tags/logs.json", "below a registry folder", () -> reader.tags(tree("data/demo/tags/logs.json", "{}")));
        assertFailure("data/demo/tags/worldgen/cold.json", "below a registry folder of worldgen",
                () -> reader.tags(tree("data/demo/tags/worldgen/cold.json", "{}")));
    }

    @Test
    void aNameOrNamespaceThatBreaksTheSyntaxIsRefusedWithTheFile() {
        assertFailure("data/Demo/recipe/a.json", "Invalid data namespace", () -> reader.recipes(tree("data/Demo/recipe/a.json", PLAIN_RECIPE)));
        assertFailure("data/demo/recipe/A.json", "Invalid resource name", () -> reader.recipes(tree("data/demo/recipe/A.json", PLAIN_RECIPE)));
    }

    @Test
    void twoFilesThatGiveTheSameResourceAreRefused() {
        Map<String, String> files = Map.of("data/demo/recipes/a.json", PLAIN_RECIPE, "data/demo/recipe/a.json", PLAIN_RECIPE);
        VersionReadException failure = assertThrows(VersionReadException.class, () -> reader.recipes(tree(files)));
        assertTrue(failure.getMessage().contains("another file gives the same resource demo/recipe/a"), failure.getMessage());
    }

    @Test
    void recipeRefusals() {
        String path = "data/demo/recipe/a.json";
        assertFailure(path, "the recipe is not an object", () -> reader.recipes(tree(path, "[]")));
        assertFailure(path, "missing key 'type' in the recipe", () -> reader.recipes(tree(path, "{}")));
        assertFailure(path, "type of the recipe is not a string", () -> reader.recipes(tree(path, "{\"type\": 3}")));
        assertFailure(path, "Invalid recipe type", () -> reader.recipes(tree(path, "{\"type\": \"smelting\"}")));
    }

    @Test
    void lootTableRefusals() {
        String path = "data/demo/loot_table/a.json";
        assertFailure(path, "the loot table is not an object", () -> reader.lootTables(tree(path, "3")));
        assertFailure(path, "type of the loot table is not a string", () -> reader.lootTables(tree(path, "{\"type\": []}")));
        assertFailure(path, "Invalid loot table type", () -> reader.lootTables(tree(path, "{\"type\": \"chest\"}")));
    }

    @Test
    void tagRefusals() {
        String path = "data/demo/tags/block/a.json";
        assertFailure(path, "the tag is not an object", () -> reader.tags(tree(path, "[]")));
        assertFailure(path, "unknown key 'extra' in the tag", () -> reader.tags(tree(path, "{\"values\": [], \"extra\": 1}")));
        assertFailure(path, "missing key 'values' in the tag", () -> reader.tags(tree(path, "{}")));
        assertFailure(path, "values of the tag is not an array", () -> reader.tags(tree(path, "{\"values\": \"x\"}")));
        assertFailure(path, "a value of the tag is not a string", () -> reader.tags(tree(path, "{\"values\": [{\"id\": \"demo:a\"}]}")));
        assertFailure(path, "replace of the tag is not a boolean", () -> reader.tags(tree(path, "{\"replace\": 1, \"values\": []}")));
        assertFailure(path, "A tag value is not empty", () -> reader.tags(tree(path, "{\"values\": [\"\"]}")));
    }

    @Test
    void theContentOfAFolderTheCallerDoesNotWantIsNotParsed() {
        VersionTree tree = tree(Map.of("data/demo/recipe/a.json", PLAIN_RECIPE, "data/demo/worldgen/broken.json", "{not json"));
        assertEquals(List.of(recipe("a")), List.copyOf(reader.recipes(tree).orElseThrow().files().keySet()));
    }
}
