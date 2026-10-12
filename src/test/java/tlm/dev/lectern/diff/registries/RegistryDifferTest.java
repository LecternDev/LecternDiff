package tlm.dev.lectern.diff.registries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.entry;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.id;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.key;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.registries;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.registry;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.ids.IdShiftClassifier;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.model.Registries;
import tlm.dev.lectern.diff.model.RegistryAdded;
import tlm.dev.lectern.diff.model.RegistryDefaultModified;
import tlm.dev.lectern.diff.model.RegistryEntryAdded;
import tlm.dev.lectern.diff.model.RegistryEntryIdModified;
import tlm.dev.lectern.diff.model.RegistryEntryIdsShifted;
import tlm.dev.lectern.diff.model.RegistryEntryRemoved;
import tlm.dev.lectern.diff.model.RegistryIdModified;
import tlm.dev.lectern.diff.model.RegistryIdsShifted;
import tlm.dev.lectern.diff.model.RegistryRemoved;

class RegistryDifferTest {

    private final RegistryDiffer differ = new RegistryDiffer(new IdShiftClassifier());

    private List<Change> diff(Registries from, Registries to) {
        List<Change> changes = List.copyOf(differ.diff(from, to));
        return new ChangeSet(release("1.0"), release("1.1"), changes).changes();
    }

    @Test
    void identicalRegistriesGiveNoChange() {
        Registries same = registries(registry("demo:gadget", 0).defaultEntry("demo:a").entry("demo:a", 0).entry("demo:b", 1).build());
        assertEquals(List.of(), diff(same, same));
    }

    @Test
    void addedRegistryIsReportedWithItsIdAndEntryCountAndNotExpandedIntoEntries() {
        Registries before = registries(registry("demo:gadget", 0).build());
        Registries after = registries(registry("demo:gadget", 0).build(),
                registry("demo:gizmo", 1).entry("demo:cog", 0).entry("demo:spring", 1).build());
        assertEquals(List.of(new RegistryAdded(key("demo:gizmo"), id(1), 2)), diff(before, after));
    }

    @Test
    void removedRegistryIsReportedWithItsIdAndEntryCount() {
        Registries before = registries(registry("demo:gadget", 0).build(),
                registry("demo:gizmo", 1).entry("demo:cog", 0).build());
        Registries after = registries(registry("demo:gadget", 0).build());
        assertEquals(List.of(new RegistryRemoved(key("demo:gizmo"), id(1), 1)), diff(before, after));
    }

    @Test
    void changedAddedAndDroppedDefaultsAreEachOneModification() {
        Registries none = registries(registry("demo:gadget", 0).entry("demo:a", 0).entry("demo:b", 1).build());
        Registries first = registries(registry("demo:gadget", 0).defaultEntry("demo:a").entry("demo:a", 0).entry("demo:b", 1).build());
        Registries second = registries(registry("demo:gadget", 0).defaultEntry("demo:b").entry("demo:a", 0).entry("demo:b", 1).build());
        assertEquals(List.of(new RegistryDefaultModified(key("demo:gadget"), Optional.empty(), Optional.of(entry("demo:a")))),
                diff(none, first));
        assertEquals(List.of(new RegistryDefaultModified(key("demo:gadget"), Optional.of(entry("demo:a")), Optional.of(entry("demo:b")))),
                diff(first, second));
        assertEquals(List.of(new RegistryDefaultModified(key("demo:gadget"), Optional.of(entry("demo:b")), Optional.empty())),
                diff(second, none));
    }

    @Test
    void swappedRegistryIdsWithoutAnyInsertionAreRealChanges() {
        Registries before = registries(registry("demo:gadget", 0).build(), registry("demo:gizmo", 1).build());
        Registries after = registries(registry("demo:gadget", 1).build(), registry("demo:gizmo", 0).build());
        assertEquals(
                List.of(new RegistryIdModified(key("demo:gadget"), id(0), id(1)),
                        new RegistryIdModified(key("demo:gizmo"), id(1), id(0))),
                diff(before, after));
    }

    @Test
    void aRegistryInsertedAtTheStartShiftsTheOthersAsOneAggregate() {
        Registries before = registries(registry("demo:gadget", 0).build(), registry("demo:gizmo", 1).build());
        Registries after = registries(registry("demo:aaa", 0).build(), registry("demo:gadget", 1).build(),
                registry("demo:gizmo", 2).build());
        assertEquals(
                List.of(new RegistryIdsShifted(2, id(0)), new RegistryAdded(key("demo:aaa"), id(0), 0)),
                diff(before, after));
    }

    @Test
    void addedAndRemovedEntriesAreReportedWithTheirIds() {
        Registries before = registries(registry("demo:gadget", 0).entry("demo:a", 0).entry("demo:b", 1).build());
        Registries after = registries(registry("demo:gadget", 0).entry("demo:a", 0).entry("demo:c", 2).build());
        assertEquals(
                List.of(new RegistryEntryAdded(key("demo:gadget"), entry("demo:c"), id(2)),
                        new RegistryEntryRemoved(key("demo:gadget"), entry("demo:b"), id(1))),
                diff(before, after));
    }

    @Test
    void swappedEntryIdsWithoutAnyInsertionAreRealChanges() {
        Registries before = registries(registry("demo:gadget", 0).entry("demo:a", 0).entry("demo:b", 1).build());
        Registries after = registries(registry("demo:gadget", 0).entry("demo:a", 1).entry("demo:b", 0).build());
        assertEquals(
                List.of(new RegistryEntryIdModified(key("demo:gadget"), entry("demo:a"), id(0), id(1)),
                        new RegistryEntryIdModified(key("demo:gadget"), entry("demo:b"), id(1), id(0))),
                diff(before, after));
    }

    @Test
    void anEntryInsertedInTheMiddleGivesOneAddedEntryAndOneAggregatedShift() {
        Registries before = registries(registry("demo:gadget", 0).entry("demo:a", 0).entry("demo:b", 1).entry("demo:c", 2).build());
        Registries after = registries(
                registry("demo:gadget", 0).entry("demo:a", 0).entry("demo:x", 1).entry("demo:b", 2).entry("demo:c", 3).build());
        assertEquals(
                List.of(new RegistryEntryAdded(key("demo:gadget"), entry("demo:x"), id(1)),
                        new RegistryEntryIdsShifted(key("demo:gadget"), 2, id(1))),
                diff(before, after));
    }

    @Test
    void aShiftAndARealIdChangeInTheSameRegistryAreReportedSeparately() {
        Registries before = registries(
                registry("demo:gadget", 0).entry("demo:a", 0).entry("demo:b", 1).entry("demo:c", 2).entry("demo:d", 3).build());
        Registries after = registries(
                registry("demo:gadget", 0).entry("demo:a", 0).entry("demo:x", 1).entry("demo:b", 2).entry("demo:c", 3)
                        .entry("demo:d", 9).build());
        assertEquals(
                List.of(new RegistryEntryAdded(key("demo:gadget"), entry("demo:x"), id(1)),
                        new RegistryEntryIdModified(key("demo:gadget"), entry("demo:d"), id(3), id(9)),
                        new RegistryEntryIdsShifted(key("demo:gadget"), 2, id(1))),
                diff(before, after));
    }

    @Test
    void aRenameIsNeverInferredItIsARemovalAndAnAddition() {
        Registries before = registries(registry("demo:old_name", 0).entry("demo:a", 0).build(),
                registry("demo:gadget", 1).entry("demo:before", 0).build());
        Registries after = registries(registry("demo:new_name", 0).entry("demo:a", 0).build(),
                registry("demo:gadget", 1).entry("demo:after", 0).build());
        assertEquals(
                List.of(new RegistryEntryAdded(key("demo:gadget"), entry("demo:after"), id(0)),
                        new RegistryEntryRemoved(key("demo:gadget"), entry("demo:before"), id(0)),
                        new RegistryAdded(key("demo:new_name"), id(0), 1),
                        new RegistryRemoved(key("demo:old_name"), id(0), 1)),
                diff(before, after));
    }

    @Test
    void theEntriesOfAnAddedOrRemovedRegistryAreNotComparedWithAnything() {
        Registries before = registries(registry("demo:gadget", 0).entry("demo:a", 0).build());
        Registries after = registries(registry("demo:gadget", 0).entry("demo:a", 0).build(),
                registry("demo:gizmo", 1).entry("demo:a", 0).entry("demo:b", 1).build());
        assertEquals(List.of(new RegistryAdded(key("demo:gizmo"), id(1), 2)), diff(before, after));
    }

    @Test
    void reversingTheComparisonMirrorsAdditionsAndRemovals() {
        Registries before = registries(registry("demo:gadget", 0).entry("demo:a", 0).build());
        Registries after = registries(registry("demo:gadget", 0).entry("demo:a", 0).entry("demo:b", 1).build());
        assertEquals(List.of(new RegistryEntryAdded(key("demo:gadget"), entry("demo:b"), id(1))), diff(before, after));
        assertEquals(List.of(new RegistryEntryRemoved(key("demo:gadget"), entry("demo:b"), id(1))), diff(after, before));
    }
}
