package tlm.dev.lectern.diff.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.json;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.lootTable;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.recipe;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.resource;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.tag;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.jsondiff.JsonPointer;
import tlm.dev.lectern.diff.jsondiff.PlaceChange;

class DataModelTest {

    @Test
    void namespacesAreLowercaseWordsWithDotsAndDashes() {
        for (String valid : List.of("minecraft", "demo", "my-pack", "a.b_c", "x1")) {
            assertDoesNotThrow(() -> new DataNamespace(valid), valid);
        }
        for (String invalid : List.of("", "Demo", "a:b", "a/b", "a b")) {
            assertThrows(IllegalArgumentException.class, () -> new DataNamespace(invalid), "'" + invalid + "'");
        }
    }

    @Test
    void resourceNamesAreSlashSeparatedSegmentsWithoutDotSegments() {
        for (String valid : List.of("acorn", "armorer/4/foo", "pack.mcmeta", "a-b/c_d/e.f", "trade_rebalance/pack.mcmeta")) {
            assertDoesNotThrow(() -> new ResourceName(valid), valid);
        }
        for (String invalid : List.of("", "/a", "a/", "a//b", "A", "a/../b", ".", "..", "a/./b", "a b", "a:b")) {
            assertThrows(IllegalArgumentException.class, () -> new ResourceName(invalid), "'" + invalid + "'");
        }
    }

    @Test
    void resourceKindsAreSnakeCaseFoldersAndTagKindsHaveTheRegistryAfterTags() {
        assertEquals("recipe", ResourceKind.RECIPE.folder());
        assertEquals(Optional.empty(), ResourceKind.RECIPE.subfolder());
        ResourceKind biomes = new ResourceKind("tags/worldgen/biome");
        assertEquals("tags", biomes.folder());
        assertEquals(Optional.of("worldgen/biome"), biomes.subfolder());
        for (String invalid : List.of("", "Tags", "tags/", "/tags", "tags//block", "a b", "a-b")) {
            assertThrows(IllegalArgumentException.class, () -> new ResourceKind(invalid), "'" + invalid + "'");
        }
    }

    @Test
    void aResourceIdIsTheNamespaceAndTheNameAndOnlyTagsHaveARegistry() {
        DataResource crate = lootTable("blocks/crate");
        assertEquals("demo:blocks/crate", crate.id());
        assertEquals(false, crate.isTag());
        DataResource logs = tag("block", "mineable/axe");
        assertEquals("demo:mineable/axe", logs.id());
        assertEquals(true, logs.isTag());
        assertEquals("block", logs.registry());
        assertThrows(IllegalStateException.class, crate::registry);
        assertEquals(false, resource("demo", "tags", "x").isTag());
    }

    @Test
    void resourcesAreOrderedByNamespaceThenKindThenName() {
        List<DataResource> sorted = List.of(
                resource("demo", "recipe", "b"), resource("alpha", "recipe", "z"), resource("demo", "loot_table", "z"),
                resource("demo", "recipe", "a")).stream().sorted().toList();
        assertEquals(List.of(resource("alpha", "recipe", "z"), resource("demo", "loot_table", "z"), resource("demo", "recipe", "a"),
                resource("demo", "recipe", "b")), sorted);
    }

    @Test
    void dataFilesAreKeptInResourceOrderWhateverTheInsertionOrder() {
        DataFiles<String> files = new DataFiles<>(Map.of(recipe("b"), "two", recipe("a"), "one", recipe("c"), "three"));
        assertEquals(List.of(recipe("a"), recipe("b"), recipe("c")), List.copyOf(files.files().keySet()));
        assertThrows(UnsupportedOperationException.class, () -> files.files().clear());
    }

    @Test
    void aTagKeepsDistinctValuesInOrderAndRefusesEmptyOnes() {
        Tag tag = new Tag(false, Set.of("demo:b", "#demo:a", "demo:a"));
        assertEquals(List.of("#demo:a", "demo:a", "demo:b"), List.copyOf(tag.values()));
        assertThrows(IllegalArgumentException.class, () -> new Tag(false, Set.of("")));
    }

    @Test
    void aRecipeNeedsANamespacedTypeAndALootTableTypeIsOptional() {
        assertDoesNotThrow(() -> new Recipe("demo:crafting_shaped", Optional.empty(), json("{}")));
        assertThrows(IllegalArgumentException.class, () -> new Recipe("crafting_shaped", Optional.empty(), json("{}")));
        assertDoesNotThrow(() -> new LootTable(Optional.empty(), json("{}")));
        assertThrows(IllegalArgumentException.class, () -> new LootTable(Optional.of("chest"), json("{}")));
    }

    @Test
    void recipeAndLootTableChangesRefuseAResourceOfAnotherKind() {
        assertThrows(IllegalArgumentException.class, () -> new RecipeAdded(lootTable("a"), "demo:smelting", Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new RecipeRemoved(tag("block", "a"), "demo:smelting", Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new LootTableAdded(recipe("a"), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new LootTableRemoved(recipe("a"), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new RecipeAdded(recipe("a"), "smelting", Optional.empty()));
    }

    @Test
    void aModifiedResourceNeedsAtLeastOnePlace() {
        assertThrows(IllegalArgumentException.class, () -> new RecipeModified(recipe("a"), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new LootTableModified(lootTable("a"), List.of()));
        List<PlaceChange> places = List.of(new PlaceChange.Added(JsonPointer.root().child("a"), json("1")));
        assertEquals(places, new RecipeModified(recipe("a"), places).places());
        assertThrows(IllegalArgumentException.class, () -> new RecipeModified(lootTable("a"), places));
    }

    @Test
    void tagChangesNeedATagResource() {
        assertThrows(IllegalArgumentException.class, () -> new TagAdded(recipe("a"), 1));
        assertThrows(IllegalArgumentException.class, () -> new TagRemoved(resource("demo", "tags", "a"), 1));
        assertThrows(IllegalArgumentException.class, () -> new TagAdded(tag("block", "a"), -1));
        assertThrows(IllegalArgumentException.class, () -> new TagReplaceModified(recipe("a"), false, true));
    }

    @Test
    void tagValueChangesAreSortedNotEmptyAndNotRepeated() {
        TagValuesAdded added = new TagValuesAdded(tag("block", "a"), List.of("demo:z", "demo:b"));
        assertEquals(List.of("demo:b", "demo:z"), added.values());
        assertThrows(IllegalArgumentException.class, () -> new TagValuesAdded(tag("block", "a"), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new TagValuesRemoved(tag("block", "a"), List.of("demo:b", "demo:b")));
    }

    @Test
    void theReplaceFlagOfATagChangeMustChange() {
        assertThrows(IllegalArgumentException.class, () -> new TagReplaceModified(tag("block", "a"), true, true));
    }

    @Test
    void aSkippedFolderNeedsAFolderNameAndAtLeastOneChangedFile() {
        assertEquals("not_compared", new SkippedFolder("worldgen", 1, 0, 0).reason().label());
        assertThrows(IllegalArgumentException.class, () -> new SkippedFolder("worldgen", 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new SkippedFolder("worldgen", -1, 2, 0));
        assertThrows(IllegalArgumentException.class, () -> new SkippedFolder("World-Gen", 1, 0, 0));
    }

    @Test
    void aSkippedDomainCanOnlyMissItsReport() {
        assertTrue(new SkippedDomain(ChangeDomain.TAGS, SkipSide.TO, new SkipReason.ReportMissing()).reason() instanceof SkipReason.ReportMissing);
        assertThrows(IllegalArgumentException.class, () -> new SkippedDomain(ChangeDomain.TAGS, SkipSide.TO, new SkipReason.NotCompared()));
    }
}
