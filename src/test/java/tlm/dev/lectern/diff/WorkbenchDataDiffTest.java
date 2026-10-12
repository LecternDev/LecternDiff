package tlm.dev.lectern.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tlm.dev.lectern.diff.fixtures.WorkbenchFixtures.requireData;
import static tlm.dev.lectern.diff.fixtures.WorkbenchFixtures.tree;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.datafolders.UncomparedFolderService;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.jsondiff.JsonComparer;
import tlm.dev.lectern.diff.loottables.LootTableDiffer;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.DataResource;
import tlm.dev.lectern.diff.model.LootTableAdded;
import tlm.dev.lectern.diff.model.RecipeAdded;
import tlm.dev.lectern.diff.model.SkippedFolder;
import tlm.dev.lectern.diff.model.TagReplaceModified;
import tlm.dev.lectern.diff.model.TagValuesAdded;
import tlm.dev.lectern.diff.model.TagValuesRemoved;
import tlm.dev.lectern.diff.read.DataReader;
import tlm.dev.lectern.diff.read.VersionTree;
import tlm.dev.lectern.diff.recipes.RecipeDiffer;
import tlm.dev.lectern.diff.tags.TagDiffer;

class WorkbenchDataDiffTest {

    private static final Set<String> COMPARED = Set.of("recipe", "loot_table", "tags");

    private final DataReader reader = new DataReader(new JsonReader());
    private final JsonComparer comparer = new JsonComparer();

    private List<? extends Change> recipes(String from, String to) {
        requireData(from, to);
        VersionTree before = tree(from);
        VersionTree after = tree(to);
        return new RecipeDiffer(comparer).diff(reader.recipes(before).orElseThrow(), reader.recipes(after).orElseThrow());
    }

    private List<? extends Change> lootTables(String from, String to) {
        requireData(from, to);
        VersionTree before = tree(from);
        VersionTree after = tree(to);
        return new LootTableDiffer(comparer).diff(reader.lootTables(before).orElseThrow(), reader.lootTables(after).orElseThrow());
    }

    private List<? extends Change> tags(String from, String to) {
        requireData(from, to);
        VersionTree before = tree(from);
        VersionTree after = tree(to);
        return new TagDiffer().diff(reader.tags(before).orElseThrow(), reader.tags(after).orElseThrow());
    }

    private List<SkippedFolder> folders(String from, String to) {
        requireData(from, to);
        return new UncomparedFolderService().summarize(reader.otherFolders(tree(from), COMPARED).orElseThrow(),
                reader.otherFolders(tree(to), COMPARED).orElseThrow());
    }

    private static Map<String, Long> countsByType(List<? extends Change> changes) {
        return changes.stream().collect(Collectors.groupingBy(change -> change.getClass().getSimpleName(), TreeMap::new, Collectors.counting()));
    }

    private static Map<String, List<Integer>> folderCounts(List<SkippedFolder> folders) {
        Map<String, List<Integer>> counts = new TreeMap<>();
        folders.forEach(folder -> counts.put(folder.folder(), List.of(folder.added(), folder.removed(), folder.modified())));
        return counts;
    }

    @Test
    void everyObservedGenerationIsReadableWithTheResourceCountsOfItsFolders() {
        Map<String, int[]> expected = Map.of(
                "1.14.4", new int[] {757, 751, 93},
                "1.20.4", new int[] {1174, 1159, 422},
                "1.21.11", new int[] {1470, 1324, 625},
                "26.3", new int[] {2042, 1447, 883},
                "26.3-rc-3", new int[] {2042, 1447, 883},
                "26.4-snapshot-3", new int[] {2055, 1450, 902});
        expected.forEach((version, counts) -> {
            requireData(version);
            VersionTree tree = tree(version);
            assertEquals(counts[0], reader.recipes(tree).orElseThrow().files().size(), version + " recipes");
            assertEquals(counts[1], reader.lootTables(tree).orElseThrow().files().size(), version + " loot tables");
            assertEquals(counts[2], reader.tags(tree).orElseThrow().files().size(), version + " tags");
        });
    }

    @Test
    void releaseToSnapshotAddsThirteenRecipesAndThreeLootTablesAndChangesNoneOfTheOthers() {
        List<? extends Change> recipes = recipes("26.3", "26.4-snapshot-3");
        assertEquals(Map.of("RecipeAdded", 13L), countsByType(recipes));
        RecipeAdded potion = recipes.stream().map(RecipeAdded.class::cast)
                .filter(added -> added.recipe().name().value().equals("brewing/lingering_potion_awkward_ice_ball")).findFirst().orElseThrow();
        assertEquals("minecraft:brewing", potion.recipeType());
        assertEquals(Optional.of("minecraft:lingering_potion"), potion.result());
        List<? extends Change> tables = lootTables("26.3", "26.4-snapshot-3");
        assertEquals(Set.of("minecraft:blocks/ice_crystal", "minecraft:blocks/icicle", "minecraft:entities/frostbite"),
                tables.stream().map(LootTableAdded.class::cast).map(added -> added.lootTable().id()).collect(Collectors.toSet()));
        assertEquals(Map.of("LootTableAdded", 3L), countsByType(tables));
    }

    @Test
    void releaseToSnapshotAddsNineteenTagsAndModifiesSeventeenByValuesOnly() {
        List<? extends Change> tags = tags("26.3", "26.4-snapshot-3");
        assertEquals(Map.of("TagAdded", 19L, "TagValuesAdded", 16L, "TagValuesRemoved", 1L), countsByType(tags));
        Set<DataResource> modified = new TreeSet<>();
        tags.stream().filter(change -> change instanceof TagValuesAdded).map(TagValuesAdded.class::cast).forEach(change -> modified.add(change.tag()));
        tags.stream().filter(change -> change instanceof TagValuesRemoved).map(TagValuesRemoved.class::cast).forEach(change -> modified.add(change.tag()));
        assertEquals(17, modified.size());
        TagValuesRemoved removed = tags.stream().filter(change -> change instanceof TagValuesRemoved).map(TagValuesRemoved.class::cast)
                .findFirst().orElseThrow();
        assertEquals(List.of("minecraft:packed_ice"), removed.values());
        assertEquals("minecraft:cannot_support_snow_layer", removed.tag().id());
        assertEquals("block", removed.tag().registry());
    }

    @Test
    void releaseToSnapshotDeclaresTheSixFoldersWithoutADomainWithTheirCounts() {
        assertEquals(
                Map.of("advancement", List.of(0, 0, 4), "block_sound_set", List.of(133, 0, 0), "datapacks", List.of(0, 0, 3),
                        "dimension_type", List.of(0, 0, 2), "villager_trade", List.of(2, 0, 0), "worldgen", List.of(49, 0, 80)),
                folderCounts(folders("26.3", "26.4-snapshot-3")));
    }

    @Test
    void releaseCandidateToReleaseChangesNothingInData() {
        assertEquals(List.of(), recipes("26.3-rc-3", "26.3"));
        assertEquals(List.of(), lootTables("26.3-rc-3", "26.3"));
        assertEquals(List.of(), tags("26.3-rc-3", "26.3"));
        assertEquals(List.of(), folders("26.3-rc-3", "26.3"));
    }

    @Test
    void oneTwentyFourToOneTwentyOneElevenKeepsTheRenamedFoldersApartFromRealAdditionsAndRemovals() {
        assertEquals(Map.of("RecipeAdded", 299L, "RecipeModified", 1159L, "RecipeRemoved", 3L), countsByType(recipes("1.20.4", "1.21.11")));
        assertEquals(Map.of("LootTableAdded", 166L, "LootTableModified", 369L, "LootTableRemoved", 1L),
                countsByType(lootTables("1.20.4", "1.21.11")));
        assertEquals(Map.of("TagAdded", 217L, "TagRemoved", 14L, "TagValuesAdded", 101L, "TagValuesRemoved", 14L),
                countsByType(tags("1.20.4", "1.21.11")));
        Map<String, List<Integer>> folders = folderCounts(folders("1.20.4", "1.21.11"));
        assertEquals(List.of(347, 34, 1225), folders.get("advancement"));
        assertEquals(List.of(), folders.keySet().stream().filter(name -> name.endsWith("s") && !name.equals("datapacks")).toList());
    }

    @Test
    void oneTwentyOneElevenToTheNewNamingChangesRecipesLootTablesTagsAndFolders() {
        assertEquals(Map.of("RecipeAdded", 585L, "RecipeModified", 981L, "RecipeRemoved", 13L), countsByType(recipes("1.21.11", "26.3")));
        assertEquals(Map.of("LootTableAdded", 123L, "LootTableModified", 1281L), countsByType(lootTables("1.21.11", "26.3")));
        assertEquals(Map.of("TagAdded", 276L, "TagRemoved", 18L, "TagValuesAdded", 80L, "TagValuesRemoved", 11L),
                countsByType(tags("1.21.11", "26.3")));
        Map<String, List<Integer>> folders = folderCounts(folders("1.21.11", "26.3"));
        assertEquals(List.of(391, 0, 0), folders.get("villager_trade"));
        assertEquals(List.of(293, 11, 1572), folders.get("advancement"));
        assertEquals(List.of(420, 228, 349), folders.get("worldgen"));
        assertEquals(List.of(91, 43, 8), folders.get("datapacks"));
    }

    @Test
    void oneFourteenFourToOneTwentyFourReadsWithoutAnyErrorAcrossTheFolderRenaming() {
        assertEquals(Map.of("RecipeAdded", 479L, "RecipeModified", 600L, "RecipeRemoved", 62L), countsByType(recipes("1.14.4", "1.20.4")));
        assertEquals(Map.of("LootTableAdded", 411L, "LootTableModified", 748L, "LootTableRemoved", 3L),
                countsByType(lootTables("1.14.4", "1.20.4")));
        assertEquals(Map.of("TagAdded", 332L, "TagRemoved", 3L, "TagValuesAdded", 46L, "TagValuesRemoved", 11L),
                countsByType(tags("1.14.4", "1.20.4")));
        assertEquals(Set.of("advancement", "chat_type", "damage_type", "datapacks", "dimension_type", "trim_material", "trim_pattern", "worldgen"),
                folderCounts(folders("1.14.4", "1.20.4")).keySet());
    }

    @Test
    void noTagReplaceFlagChangesOnTheObservedPairsBecauseAnAbsentFlagIsFalse() {
        for (List<String> pair : List.of(List.of("1.14.4", "1.20.4"), List.of("1.20.4", "1.21.11"), List.of("1.21.11", "26.3"))) {
            assertTrue(tags(pair.get(0), pair.get(1)).stream().noneMatch(change -> change instanceof TagReplaceModified), pair.toString());
        }
    }

    private Set<String> tagRegistries(String version) {
        requireData(version);
        return reader.tags(tree(version)).orElseThrow().files().keySet().stream().map(DataResource::registry)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Test
    void tagRegistriesAreSingularWhateverTheGenerationAndWorldgenRegistriesHaveTwoLevels() {
        assertEquals(Set.of("block", "entity_type", "fluid", "item"), tagRegistries("1.14.4"));
        assertEquals(Set.of("banner_pattern", "block", "cat_variant", "damage_type", "entity_type", "fluid", "game_event", "instrument", "item",
                "painting_variant", "point_of_interest_type", "worldgen/biome", "worldgen/flat_level_generator_preset", "worldgen/structure",
                "worldgen/world_preset"), tagRegistries("1.20.4"));
        assertTrue(tagRegistries("26.4-snapshot-3").containsAll(Set.of("block", "block_sound_set", "potion", "villager_trade", "worldgen/feature")));
    }
}
