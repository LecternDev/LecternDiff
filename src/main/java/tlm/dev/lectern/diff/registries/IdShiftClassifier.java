package tlm.dev.lectern.diff.registries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import tlm.dev.lectern.diff.model.ProtocolId;

public final class IdShiftClassifier {

    public record Modification<K>(K key, ProtocolId oldId, ProtocolId newId) {
    }

    public record Result<K>(int shiftedCount, Optional<ProtocolId> firstShiftedId, List<Modification<K>> modified) {

        public Result {
            modified = List.copyOf(modified);
        }
    }

    public <K extends Comparable<K>> Result<K> classify(Map<K, ProtocolId> from, Map<K, ProtocolId> to) {
        int[] addedIds = sortedIds(to, from.keySet());
        int[] removedIds = sortedIds(from, to.keySet());
        int shifted = 0;
        int firstShifted = Integer.MAX_VALUE;
        List<Modification<K>> modified = new ArrayList<>();
        for (K key : new TreeSet<>(from.keySet())) {
            ProtocolId newId = to.get(key);
            if (newId == null) {
                continue;
            }
            ProtocolId oldId = from.get(key);
            if (oldId.equals(newId)) {
                continue;
            }
            int expected = countBelow(addedIds, newId.value()) - countBelow(removedIds, oldId.value());
            if (newId.value() - oldId.value() == expected) {
                shifted++;
                firstShifted = Math.min(firstShifted, oldId.value());
            } else {
                modified.add(new Modification<>(key, oldId, newId));
            }
        }
        Optional<ProtocolId> first = shifted == 0 ? Optional.empty() : Optional.of(new ProtocolId(firstShifted));
        return new Result<>(shifted, first, modified);
    }

    private static <K> int[] sortedIds(Map<K, ProtocolId> source, Set<K> excluded) {
        return source.entrySet().stream()
                .filter(entry -> !excluded.contains(entry.getKey()))
                .mapToInt(entry -> entry.getValue().value())
                .sorted()
                .toArray();
    }

    private static int countBelow(int[] sorted, int bound) {
        int found = Arrays.binarySearch(sorted, bound);
        return found >= 0 ? found : -found - 1;
    }
}
