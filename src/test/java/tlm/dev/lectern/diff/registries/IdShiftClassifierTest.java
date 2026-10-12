package tlm.dev.lectern.diff.registries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.entry;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.id;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.model.EntryKey;
import tlm.dev.lectern.diff.model.ProtocolId;

class IdShiftClassifierTest {

    private final IdShiftClassifier classifier = new IdShiftClassifier();

    private static Map<EntryKey, ProtocolId> contiguous(String... namesInIdOrder) {
        Map<EntryKey, ProtocolId> ids = new TreeMap<>();
        for (int index = 0; index < namesInIdOrder.length; index++) {
            ids.put(entry("demo:" + namesInIdOrder[index]), id(index));
        }
        return ids;
    }

    private static IdShiftClassifier.Modification<EntryKey> modification(String name, int oldId, int newId) {
        return new IdShiftClassifier.Modification<>(entry("demo:" + name), id(oldId), id(newId));
    }

    @Test
    void nothingChangedGivesAnEmptyResult() {
        IdShiftClassifier.Result<EntryKey> result = classifier.classify(contiguous("a", "b"), contiguous("a", "b"));
        assertEquals(new IdShiftClassifier.Result<EntryKey>(0, Optional.empty(), List.of()), result);
    }

    @Test
    void insertingInTheMiddleShiftsEveryLaterEntryAsOneAggregate() {
        IdShiftClassifier.Result<EntryKey> result =
                classifier.classify(contiguous("a", "b", "c", "d"), contiguous("a", "x", "b", "c", "d"));
        assertEquals(3, result.shiftedCount());
        assertEquals(Optional.of(id(1)), result.firstShiftedId());
        assertEquals(List.of(), result.modified());
    }

    @Test
    void removingShiftsEveryLaterEntryDown() {
        IdShiftClassifier.Result<EntryKey> result =
                classifier.classify(contiguous("a", "b", "c", "d"), contiguous("a", "c", "d"));
        assertEquals(2, result.shiftedCount());
        assertEquals(Optional.of(id(2)), result.firstShiftedId());
        assertEquals(List.of(), result.modified());
    }

    @Test
    void addingAtTheEndShiftsNothing() {
        IdShiftClassifier.Result<EntryKey> result = classifier.classify(contiguous("a", "b"), contiguous("a", "b", "x"));
        assertEquals(0, result.shiftedCount());
        assertEquals(Optional.empty(), result.firstShiftedId());
        assertEquals(List.of(), result.modified());
    }

    @Test
    void replacingAnEntryByAnotherAtTheSameIdShiftsNothing() {
        IdShiftClassifier.Result<EntryKey> result = classifier.classify(contiguous("a", "b", "c"), contiguous("a", "x", "c"));
        assertEquals(0, result.shiftedCount());
        assertEquals(List.of(), result.modified());
    }

    @Test
    void aRemovalAndAnAdditionAtDifferentPlacesCombineIntoTheRightShift() {
        IdShiftClassifier.Result<EntryKey> result =
                classifier.classify(contiguous("a", "b", "c", "d", "e"), contiguous("b", "c", "x", "d", "e"));
        assertEquals(2, result.shiftedCount());
        assertEquals(Optional.of(id(1)), result.firstShiftedId());
        assertEquals(List.of(), result.modified());
    }

    @Test
    void aRemovalFollowedByAnAdditionAtTheEndShiftsTheEntriesInBetween() {
        IdShiftClassifier.Result<EntryKey> result =
                classifier.classify(contiguous("a", "b", "c", "d", "e"), contiguous("a", "c", "d", "e", "x"));
        assertEquals(3, result.shiftedCount());
        assertEquals(Optional.of(id(2)), result.firstShiftedId());
        assertEquals(List.of(), result.modified());
    }

    @Test
    void aSwapWithoutAnyInsertionOrRemovalIsARealChangeForEachEntry() {
        Map<EntryKey, ProtocolId> before = Map.of(entry("demo:a"), id(0), entry("demo:b"), id(1));
        Map<EntryKey, ProtocolId> after = Map.of(entry("demo:a"), id(1), entry("demo:b"), id(0));
        IdShiftClassifier.Result<EntryKey> result = classifier.classify(before, after);
        assertEquals(0, result.shiftedCount());
        assertEquals(List.of(modification("a", 0, 1), modification("b", 1, 0)), result.modified());
    }

    @Test
    void aRealIdChangeNextToAShiftStaysAChange() {
        Map<EntryKey, ProtocolId> after = new TreeMap<>(contiguous("a", "x", "b", "c", "d"));
        after.put(entry("demo:d"), id(9));
        IdShiftClassifier.Result<EntryKey> result = classifier.classify(contiguous("a", "b", "c", "d"), after);
        assertEquals(2, result.shiftedCount());
        assertEquals(Optional.of(id(1)), result.firstShiftedId());
        assertEquals(List.of(modification("d", 3, 9)), result.modified());
    }

    @Test
    void idsWithHolesAreHandledLikeAnyOtherIds() {
        Map<EntryKey, ProtocolId> before = Map.of(entry("demo:a"), id(1), entry("demo:b"), id(5), entry("demo:c"), id(9));
        Map<EntryKey, ProtocolId> after = Map.of(entry("demo:a"), id(1), entry("demo:x"), id(3), entry("demo:b"), id(5),
                entry("demo:c"), id(9));
        IdShiftClassifier.Result<EntryKey> result = classifier.classify(before, after);
        assertEquals(0, result.shiftedCount());
        assertEquals(List.of(), result.modified());
    }

    @Test
    void theResultDoesNotDependOnTheIterationOrderOfTheInputMaps() {
        Map<EntryKey, ProtocolId> before = new HashMap<>(Map.of(entry("demo:a"), id(0), entry("demo:b"), id(1),
                entry("demo:c"), id(2), entry("demo:d"), id(3)));
        Map<EntryKey, ProtocolId> after = new HashMap<>(Map.of(entry("demo:a"), id(3), entry("demo:b"), id(2),
                entry("demo:c"), id(1), entry("demo:d"), id(0)));
        List<IdShiftClassifier.Modification<EntryKey>> modified = classifier.classify(before, after).modified();
        assertEquals(List.of(modification("a", 0, 3), modification("b", 1, 2), modification("c", 2, 1),
                modification("d", 3, 0)), modified);
    }
}
