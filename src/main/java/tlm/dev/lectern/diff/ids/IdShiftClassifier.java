package tlm.dev.lectern.diff.ids;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;

public final class IdShiftClassifier {

    public record Modification<K>(K key, int oldId, int newId) {
    }

    public record Result<K>(List<K> shifted, OptionalInt firstShiftedId, List<Modification<K>> modified) {

        public Result {
            shifted = List.copyOf(shifted);
            modified = List.copyOf(modified);
        }

        public int shiftedCount() {
            return shifted.size();
        }
    }

    public <K extends Comparable<K>> Result<K> classify(Map<K, Integer> from, Map<K, Integer> to) {
        int[] addedIds = sortedIds(to, from.keySet());
        int[] removedIds = sortedIds(from, to.keySet());
        List<K> shifted = new ArrayList<>();
        int firstShifted = Integer.MAX_VALUE;
        List<Modification<K>> modified = new ArrayList<>();
        for (K key : new TreeSet<>(from.keySet())) {
            Integer newId = to.get(key);
            if (newId == null) {
                continue;
            }
            int oldId = from.get(key);
            if (oldId == newId) {
                continue;
            }
            int expected = countBelow(addedIds, newId) - countBelow(removedIds, oldId);
            if (newId - oldId == expected) {
                shifted.add(key);
                firstShifted = Math.min(firstShifted, oldId);
            } else {
                modified.add(new Modification<>(key, oldId, newId));
            }
        }
        OptionalInt first = shifted.isEmpty() ? OptionalInt.empty() : OptionalInt.of(firstShifted);
        return new Result<>(shifted, first, modified);
    }

    private static <K> int[] sortedIds(Map<K, Integer> source, Set<K> excluded) {
        return source.entrySet().stream()
                .filter(entry -> !excluded.contains(entry.getKey()))
                .mapToInt(Map.Entry::getValue)
                .sorted()
                .toArray();
    }

    private static int countBelow(int[] sorted, int bound) {
        int found = Arrays.binarySearch(sorted, bound);
        return found >= 0 ? found : -found - 1;
    }
}
