package tlm.dev.lectern.diff.ids;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class IdShiftClassifierTest {

    private final IdShiftClassifier classifier = new IdShiftClassifier();

    private static Map<String, Integer> contiguous(String... namesInIdOrder) {
        Map<String, Integer> ids = new TreeMap<>();
        for (int index = 0; index < namesInIdOrder.length; index++) {
            ids.put(namesInIdOrder[index], index);
        }
        return ids;
    }

    private static IdShiftClassifier.Modification<String> modification(String key, int oldId, int newId) {
        return new IdShiftClassifier.Modification<>(key, oldId, newId);
    }

    @Test
    void nothingChangedGivesAnEmptyResult() {
        IdShiftClassifier.Result<String> result = classifier.classify(contiguous("a", "b"), contiguous("a", "b"));
        assertEquals(new IdShiftClassifier.Result<String>(List.of(), OptionalInt.empty(), List.of()), result);
    }

    @Test
    void insertingInTheMiddleShiftsEveryLaterKeyAsOneAggregate() {
        IdShiftClassifier.Result<String> result =
                classifier.classify(contiguous("a", "b", "c", "d"), contiguous("a", "x", "b", "c", "d"));
        assertEquals(List.of("b", "c", "d"), result.shifted());
        assertEquals(3, result.shiftedCount());
        assertEquals(OptionalInt.of(1), result.firstShiftedId());
        assertEquals(List.of(), result.modified());
    }

    @Test
    void removingShiftsEveryLaterKeyDown() {
        IdShiftClassifier.Result<String> result =
                classifier.classify(contiguous("a", "b", "c", "d"), contiguous("a", "c", "d"));
        assertEquals(List.of("c", "d"), result.shifted());
        assertEquals(OptionalInt.of(2), result.firstShiftedId());
        assertEquals(List.of(), result.modified());
    }

    @Test
    void addingAtTheEndShiftsNothing() {
        IdShiftClassifier.Result<String> result = classifier.classify(contiguous("a", "b"), contiguous("a", "b", "x"));
        assertEquals(0, result.shiftedCount());
        assertEquals(OptionalInt.empty(), result.firstShiftedId());
        assertEquals(List.of(), result.modified());
    }

    @Test
    void replacingAKeyByAnotherAtTheSameIdShiftsNothing() {
        IdShiftClassifier.Result<String> result = classifier.classify(contiguous("a", "b", "c"), contiguous("a", "x", "c"));
        assertEquals(0, result.shiftedCount());
        assertEquals(List.of(), result.modified());
    }

    @Test
    void aRemovalAndAnAdditionAtDifferentPlacesCombineIntoTheRightShift() {
        IdShiftClassifier.Result<String> result =
                classifier.classify(contiguous("a", "b", "c", "d", "e"), contiguous("b", "c", "x", "d", "e"));
        assertEquals(List.of("b", "c"), result.shifted());
        assertEquals(OptionalInt.of(1), result.firstShiftedId());
        assertEquals(List.of(), result.modified());
    }

    @Test
    void aRemovalFollowedByAnAdditionAtTheEndShiftsTheKeysInBetween() {
        IdShiftClassifier.Result<String> result =
                classifier.classify(contiguous("a", "b", "c", "d", "e"), contiguous("a", "c", "d", "e", "x"));
        assertEquals(List.of("c", "d", "e"), result.shifted());
        assertEquals(OptionalInt.of(2), result.firstShiftedId());
        assertEquals(List.of(), result.modified());
    }

    @Test
    void aSwapWithoutAnyInsertionOrRemovalIsARealChangeForEachKey() {
        IdShiftClassifier.Result<String> result = classifier.classify(Map.of("a", 0, "b", 1), Map.of("a", 1, "b", 0));
        assertEquals(0, result.shiftedCount());
        assertEquals(List.of(modification("a", 0, 1), modification("b", 1, 0)), result.modified());
    }

    @Test
    void aRealIdChangeNextToAShiftStaysAChange() {
        Map<String, Integer> after = new TreeMap<>(contiguous("a", "x", "b", "c", "d"));
        after.put("d", 9);
        IdShiftClassifier.Result<String> result = classifier.classify(contiguous("a", "b", "c", "d"), after);
        assertEquals(List.of("b", "c"), result.shifted());
        assertEquals(OptionalInt.of(1), result.firstShiftedId());
        assertEquals(List.of(modification("d", 3, 9)), result.modified());
    }

    @Test
    void idsWithHolesAreHandledLikeAnyOtherIds() {
        IdShiftClassifier.Result<String> result =
                classifier.classify(Map.of("a", 1, "b", 5, "c", 9), Map.of("a", 1, "x", 3, "b", 5, "c", 9));
        assertEquals(0, result.shiftedCount());
        assertEquals(List.of(), result.modified());
    }

    @Test
    void theResultDoesNotDependOnTheIterationOrderOfTheInputMaps() {
        Map<String, Integer> before = new HashMap<>(Map.of("a", 0, "b", 1, "c", 2, "d", 3));
        Map<String, Integer> after = new HashMap<>(Map.of("a", 3, "b", 2, "c", 1, "d", 0));
        assertEquals(
                List.of(modification("a", 0, 3), modification("b", 1, 2), modification("c", 2, 1), modification("d", 3, 0)),
                classifier.classify(before, after).modified());
    }
}
