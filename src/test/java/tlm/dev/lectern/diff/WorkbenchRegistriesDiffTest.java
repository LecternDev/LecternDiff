package tlm.dev.lectern.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.entry;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.id;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.key;
import static tlm.dev.lectern.diff.fixtures.WorkbenchFixtures.requireReport;
import static tlm.dev.lectern.diff.fixtures.WorkbenchFixtures.tree;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.json.CanonicalJsonWriter;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.model.Registries;
import tlm.dev.lectern.diff.model.RegistryAdded;
import tlm.dev.lectern.diff.model.RegistryEntryAdded;
import tlm.dev.lectern.diff.model.RegistryEntryIdModified;
import tlm.dev.lectern.diff.model.RegistryEntryIdsShifted;
import tlm.dev.lectern.diff.model.RegistryEntryRemoved;
import tlm.dev.lectern.diff.model.RegistryRemoved;
import tlm.dev.lectern.diff.model.VersionKind;
import tlm.dev.lectern.diff.read.RegistryReportReader;
import tlm.dev.lectern.diff.read.VersionTree;
import tlm.dev.lectern.diff.read.VersionTree;
import tlm.dev.lectern.diff.ids.IdShiftClassifier;
import tlm.dev.lectern.diff.registries.RegistryDiffer;
import tlm.dev.lectern.diff.write.BlockChangeRenderer;
import tlm.dev.lectern.diff.write.ChangeSetWriter;
import tlm.dev.lectern.diff.write.RegistryChangeRenderer;

class WorkbenchRegistriesDiffTest {

    private static final String RELEASE = "26.3";
    private static final String SNAPSHOT = "26.4-snapshot-3";

    private final JsonReader json = new JsonReader();
    private final RegistryReportReader reader = new RegistryReportReader(json);
    private final RegistryDiffer differ = new RegistryDiffer(new IdShiftClassifier());

    private ChangeSet diff(String from, String to) {
        requireReport(RegistryReportReader.FILE, from, to);
        VersionTree before = tree(from);
        VersionTree after = tree(to);
        List<Change> changes = List.copyOf(differ.diff(reader.read(before), reader.read(after)));
        return new ChangeSet(before.version(), after.version(), changes);
    }

    private static <T extends Change> List<T> ofType(ChangeSet changeSet, Class<T> type) {
        return changeSet.changes().stream().filter(type::isInstance).map(type::cast).toList();
    }

    private static Map<String, Integer> countsByRegistry(List<RegistryEntryAdded> added) {
        Map<String, Integer> counts = new TreeMap<>();
        added.forEach(change -> counts.merge(change.registry().value(), 1, Integer::sum));
        return counts;
    }

    private Set<String> registryKeys(String version) {
        String text = tree(version).readText(RegistryReportReader.FILE).orElseThrow();
        return new TreeSet<>(((JsonObject) json.read(text)).members().keySet());
    }

    @Test
    void everyObservedGenerationOfTheReportIsReadable() {
        Map<String, Integer> expectedRegistryCounts = Map.of(
                "1.14.4", 37, "1.20.4", 70, "1.21.11", 95, RELEASE, 95, SNAPSHOT, 95);
        expectedRegistryCounts.keySet().forEach(version -> requireReport(RegistryReportReader.FILE, version));
        expectedRegistryCounts.forEach((version, count) -> {
            Registries registries = reader.read(tree(version));
            assertEquals(count, registries.byKey().size(), version);
        });
    }

    @Test
    void releaseToSnapshotReportsTheRenamedRegistryAsOneRemovalAndOneAddition() {
        ChangeSet changeSet = diff(RELEASE, SNAPSHOT);
        assertEquals(new VersionKind.Release(), changeSet.from().kind());
        assertEquals(new VersionKind.Snapshot(), changeSet.to().kind());
        assertEquals(List.of(new RegistryRemoved(key("minecraft:rule_test"), id(13), 10)), ofType(changeSet, RegistryRemoved.class));
        assertEquals(List.of(new RegistryAdded(key("minecraft:rule_test_type"), id(13), 10)), ofType(changeSet, RegistryAdded.class));
    }

    @Test
    void releaseToSnapshotReportsTheRenamedEntriesAsRemovalsAndAdditions() {
        ChangeSet changeSet = diff(RELEASE, SNAPSHOT);
        List<RegistryEntryRemoved> removed = ofType(changeSet, RegistryEntryRemoved.class);
        assertEquals(
                Set.of("minecraft:particle_type/minecraft:item_snowball", "minecraft:worldgen/feature_type/minecraft:large_dripstone"),
                removed.stream().map(change -> change.registry() + "/" + change.entry()).collect(Collectors.toSet()));
        assertEquals(2, removed.size());
        List<RegistryEntryAdded> added = ofType(changeSet, RegistryEntryAdded.class);
        assertTrue(added.stream().anyMatch(change -> change.registry().equals(key("minecraft:worldgen/feature_type"))
                && change.entry().equals(entry("minecraft:large_speleothem"))));
    }

    @Test
    void releaseToSnapshotAddsTheNewItemsBlocksEntitiesAndSounds() {
        ChangeSet changeSet = diff(RELEASE, SNAPSHOT);
        List<RegistryEntryAdded> added = ofType(changeSet, RegistryEntryAdded.class);
        Map<String, Integer> expected = new TreeMap<>(Map.ofEntries(
                Map.entry("minecraft:block", 2),
                Map.entry("minecraft:block_predicate_type", 1),
                Map.entry("minecraft:chunk_status", 1),
                Map.entry("minecraft:entity_type", 2),
                Map.entry("minecraft:environment_attribute", 1),
                Map.entry("minecraft:item", 4),
                Map.entry("minecraft:mob_effect", 1),
                Map.entry("minecraft:particle_type", 1),
                Map.entry("minecraft:potion", 2),
                Map.entry("minecraft:sound_event", 26),
                Map.entry("minecraft:worldgen/feature_type", 1)));
        assertEquals(expected, countsByRegistry(added));
        assertEquals(Set.of("minecraft:ice_crystal", "minecraft:icicle"), entriesAddedTo(added, "minecraft:block"));
        assertEquals(Set.of("minecraft:frostbite_spawn_egg", "minecraft:ice_ball", "minecraft:ice_crystal", "minecraft:icicle"),
                entriesAddedTo(added, "minecraft:item"));
        assertEquals(Set.of("minecraft:frostbite", "minecraft:ice_ball"), entriesAddedTo(added, "minecraft:entity_type"));
    }

    private static Set<String> entriesAddedTo(List<RegistryEntryAdded> added, String registry) {
        return added.stream()
                .filter(change -> change.registry().value().equals(registry))
                .map(change -> change.entry().value())
                .collect(Collectors.toSet());
    }

    @Test
    void releaseToSnapshotAggregatesTheIdShiftsInsteadOfListingEveryEntry() {
        ChangeSet changeSet = diff(RELEASE, SNAPSHOT);
        Map<String, RegistryEntryIdsShifted> shifts = ofType(changeSet, RegistryEntryIdsShifted.class).stream()
                .collect(Collectors.toMap(shift -> shift.registry().value(), Function.identity()));
        assertEquals(Set.of("minecraft:block", "minecraft:chunk_status", "minecraft:entity_type",
                "minecraft:environment_attribute", "minecraft:item", "minecraft:particle_type", "minecraft:sound_event"),
                shifts.keySet());
        assertEquals(525, shifts.get("minecraft:item").count());
        assertEquals(id(1133), shifts.get("minecraft:item").firstId());
        assertEquals(104, shifts.get("minecraft:entity_type").count());
        assertEquals(id(57), shifts.get("minecraft:entity_type").firstId());
        assertEquals(217, shifts.get("minecraft:block").count());
        assertEquals(7, shifts.get("minecraft:chunk_status").count());
        assertEquals(34, shifts.get("minecraft:environment_attribute").count());
        assertEquals(33, shifts.get("minecraft:particle_type").count());
        assertEquals(93, shifts.get("minecraft:sound_event").count());
    }

    @Test
    void releaseToSnapshotKeepsTheTwoRealIdChangesOfTheSwappedEntries() {
        ChangeSet changeSet = diff(RELEASE, SNAPSHOT);
        assertEquals(
                List.of(new RegistryEntryIdModified(key("minecraft:point_of_interest_type"), entry("minecraft:lightning_rod"), id(20), id(19)),
                        new RegistryEntryIdModified(key("minecraft:point_of_interest_type"), entry("minecraft:test_instance"), id(19), id(20))),
                ofType(changeSet, RegistryEntryIdModified.class));
    }

    @Test
    void releaseToSnapshotHasNoOtherChange() {
        ChangeSet changeSet = diff(RELEASE, SNAPSHOT);
        assertEquals(2 + 42 + 2 + 7 + 2, changeSet.changes().size());
    }

    @Test
    void reversingTheDiffSwapsAdditionsAndRemovals() {
        ChangeSet forward = diff(RELEASE, SNAPSHOT);
        ChangeSet backward = diff(SNAPSHOT, RELEASE);
        Set<String> addedForward = ofType(forward, RegistryEntryAdded.class).stream()
                .map(change -> change.registry() + "/" + change.entry()).collect(Collectors.toSet());
        Set<String> removedBackward = ofType(backward, RegistryEntryRemoved.class).stream()
                .map(change -> change.registry() + "/" + change.entry()).collect(Collectors.toSet());
        assertEquals(addedForward, removedBackward);
        assertEquals(forward.changes().size(), backward.changes().size());
    }

    @Test
    void theWrittenChangeSetIsCanonicalAndStable() {
        ChangeSet changeSet = diff(RELEASE, SNAPSHOT);
        ChangeSetWriter writer = new ChangeSetWriter(new RegistryChangeRenderer(), new BlockChangeRenderer(), new CanonicalJsonWriter());
        String first = writer.write(changeSet);
        assertEquals(first, writer.write(diff(RELEASE, SNAPSHOT)));
        assertEquals(first, new CanonicalJsonWriter().write(json.read(first)));
    }

    @Test
    void theRegistriesAddedAcrossGenerationsAreExactlyTheDifferenceOfTheKeySets() {
        for (List<String> pair : List.of(List.of("1.14.4", "1.20.4"), List.of("1.20.4", "1.21.11"))) {
            ChangeSet changeSet = diff(pair.get(0), pair.get(1));
            Set<String> before = registryKeys(pair.get(0));
            Set<String> after = registryKeys(pair.get(1));
            Set<String> expectedAdded = new TreeSet<>(after);
            expectedAdded.removeAll(before);
            Set<String> expectedRemoved = new TreeSet<>(before);
            expectedRemoved.removeAll(after);
            Set<String> added = ofType(changeSet, RegistryAdded.class).stream()
                    .map(change -> change.registry().value()).collect(Collectors.toCollection(TreeSet::new));
            Set<String> removed = ofType(changeSet, RegistryRemoved.class).stream()
                    .map(change -> change.registry().value()).collect(Collectors.toCollection(TreeSet::new));
            assertEquals(expectedAdded, added, pair + " added");
            assertEquals(expectedRemoved, removed, pair + " removed");
        }
    }
}
