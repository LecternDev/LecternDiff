package tlm.dev.lectern.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.blockKey;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.stateId;
import static tlm.dev.lectern.diff.fixtures.WorkbenchFixtures.requireReport;
import static tlm.dev.lectern.diff.fixtures.WorkbenchFixtures.tree;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.blocks.BlockDiffer;
import tlm.dev.lectern.diff.ids.IdShiftClassifier;
import tlm.dev.lectern.diff.json.CanonicalJsonWriter;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.model.BlockAdded;
import tlm.dev.lectern.diff.model.BlockRemoved;
import tlm.dev.lectern.diff.model.BlockStateIdsModified;
import tlm.dev.lectern.diff.model.BlockStateIdsShifted;
import tlm.dev.lectern.diff.model.Blocks;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.read.BlockReportReader;
import tlm.dev.lectern.diff.read.VersionTree;
import tlm.dev.lectern.diff.write.BlockChangeRenderer;
import tlm.dev.lectern.diff.write.ChangeSetWriter;
import tlm.dev.lectern.diff.write.RegistryChangeRenderer;

class WorkbenchBlocksDiffTest {

    private static final String RELEASE = "26.3";
    private static final String SNAPSHOT = "26.4-snapshot-3";

    private final JsonReader json = new JsonReader();
    private final BlockReportReader reader = new BlockReportReader(json);
    private final BlockDiffer differ = new BlockDiffer(new IdShiftClassifier());

    private ChangeSet diff(String from, String to) {
        requireReport(BlockReportReader.FILE, from, to);
        VersionTree before = tree(from);
        VersionTree after = tree(to);
        List<Change> changes = List.copyOf(differ.diff(reader.read(before).orElseThrow(), reader.read(after).orElseThrow()));
        return new ChangeSet(before.version(), after.version(), changes);
    }

    private static <T extends Change> List<T> ofType(ChangeSet changeSet, Class<T> type) {
        return changeSet.changes().stream().filter(type::isInstance).map(type::cast).toList();
    }

    private static Map<String, Long> countsByType(ChangeSet changeSet) {
        return changeSet.changes().stream()
                .collect(Collectors.groupingBy(change -> change.getClass().getSimpleName(), TreeMap::new, Collectors.counting()));
    }

    private Set<String> blockKeys(String version) {
        String text = tree(version).readText(BlockReportReader.FILE).orElseThrow();
        return new TreeSet<>(((JsonObject) json.read(text)).members().keySet());
    }

    private void assertBlockSetsDifferByExactlyTheKeySets(String from, String to) {
        ChangeSet changeSet = diff(from, to);
        Set<String> before = blockKeys(from);
        Set<String> after = blockKeys(to);
        Set<String> expectedAdded = new TreeSet<>(after);
        expectedAdded.removeAll(before);
        Set<String> expectedRemoved = new TreeSet<>(before);
        expectedRemoved.removeAll(after);
        assertEquals(expectedAdded, ofType(changeSet, BlockAdded.class).stream()
                .map(change -> change.block().value()).collect(Collectors.toCollection(TreeSet::new)), from + " -> " + to + " added");
        assertEquals(expectedRemoved, ofType(changeSet, BlockRemoved.class).stream()
                .map(change -> change.block().value()).collect(Collectors.toCollection(TreeSet::new)), from + " -> " + to + " removed");
    }

    @Test
    void everyObservedGenerationOfTheReportIsReadable() {
        Map<String, int[]> expected = Map.of(
                "1.14.4", new int[] {676, 11271},
                "1.20.4", new int[] {1058, 26644},
                "1.21.11", new int[] {1166, 29671},
                RELEASE, new int[] {1286, 35723},
                SNAPSHOT, new int[] {1288, 35775});
        expected.keySet().forEach(version -> requireReport(BlockReportReader.FILE, version));
        expected.forEach((version, counts) -> {
            Blocks blocks = reader.read(tree(version)).orElseThrow();
            assertEquals(counts[0], blocks.byKey().size(), version + " blocks");
            assertEquals(counts[1], blocks.stateCount(), version + " states");
        });
    }

    @Test
    void releaseToSnapshotIsTwoNewBlocksAndOneAggregatedShiftOfTheExistingBlocks() {
        ChangeSet changeSet = diff(RELEASE, SNAPSHOT);
        assertEquals(
                List.of(new BlockStateIdsShifted(8963, 217, stateId(26760)),
                        new BlockAdded(blockKey("minecraft:ice_crystal"), 12),
                        new BlockAdded(blockKey("minecraft:icicle"), 40)),
                changeSet.changes());
    }

    @Test
    void releaseToSnapshotHasNoRealStateIdChange() {
        assertEquals(List.of(), ofType(diff(RELEASE, SNAPSHOT), BlockStateIdsModified.class));
    }

    @Test
    void reversingTheDiffSwapsAdditionsAndRemovals() {
        ChangeSet backward = diff(SNAPSHOT, RELEASE);
        assertEquals(
                List.of(new BlockRemoved(blockKey("minecraft:ice_crystal"), 12), new BlockRemoved(blockKey("minecraft:icicle"), 40)),
                ofType(backward, BlockRemoved.class));
        assertEquals(List.of(), ofType(backward, BlockAdded.class));
        assertEquals(1, ofType(backward, BlockStateIdsShifted.class).size());
    }

    @Test
    void theWrittenChangeSetIsCanonicalAndStable() {
        ChangeSetWriter writer = Composition.changeSetWriter();
        String first = writer.write(diff(RELEASE, SNAPSHOT));
        assertEquals(first, writer.write(diff(RELEASE, SNAPSHOT)));
        assertEquals(first, new CanonicalJsonWriter().write(json.read(first)));
    }

    @Test
    void theBlocksAddedAndRemovedAcrossGenerationsAreExactlyTheDifferenceOfTheKeySets() {
        assertBlockSetsDifferByExactlyTheKeySets("1.14.4", "1.20.4");
        assertBlockSetsDifferByExactlyTheKeySets("1.20.4", "1.21.11");
        assertBlockSetsDifferByExactlyTheKeySets("1.21.11", RELEASE);
    }

    @Test
    void oldToNewGenerationCountsEveryKindOfBlockChange() {
        ChangeSet changeSet = diff("1.14.4", "1.20.4");
        assertEquals(Map.ofEntries(
                Map.entry("BlockAdded", 384L), Map.entry("BlockRemoved", 2L), Map.entry("BlockPropertyAdded", 26L),
                Map.entry("BlockPropertyRemoved", 2L), Map.entry("BlockPropertyValuesAdded", 57L),
                Map.entry("BlockPropertyValuesRemoved", 56L), Map.entry("BlockDefaultStateModified", 15L),
                Map.entry("BlockStateIdsModified", 46L), Map.entry("BlockStateIdsShifted", 1L)), countsByType(changeSet));
        assertEquals(List.of(new BlockStateIdsShifted(9323, 569, stateId(20))), ofType(changeSet, BlockStateIdsShifted.class));
        assertEquals(751, ofType(changeSet, BlockStateIdsModified.class).stream().mapToInt(BlockStateIdsModified::stateCount).sum());
    }

    @Test
    void theGenerationWithADefinitionFieldIsComparedWithItsNeighbours() {
        ChangeSet before = diff("1.20.4", "1.21.11");
        assertEquals(Map.ofEntries(
                Map.entry("BlockAdded", 109L), Map.entry("BlockRemoved", 1L), Map.entry("BlockPropertyAdded", 1L),
                Map.entry("BlockStateIdsShifted", 1L)), countsByType(before));
        assertEquals(List.of(new BlockStateIdsShifted(26610, 1036, stateId(22))), ofType(before, BlockStateIdsShifted.class));
        ChangeSet after = diff("1.21.11", RELEASE);
        assertEquals(Map.ofEntries(
                Map.entry("BlockAdded", 120L), Map.entry("BlockPropertyValuesAdded", 1L),
                Map.entry("BlockDefaultStateModified", 40L), Map.entry("BlockStateIdsModified", 96L),
                Map.entry("BlockStateIdsShifted", 1L)), countsByType(after));
        assertEquals(List.of(new BlockStateIdsShifted(27090, 1047, stateId(27))), ofType(after, BlockStateIdsShifted.class));
        assertEquals(2554, ofType(after, BlockStateIdsModified.class).stream().mapToInt(BlockStateIdsModified::stateCount).sum());
    }
}
