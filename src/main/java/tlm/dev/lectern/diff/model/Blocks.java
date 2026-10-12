package tlm.dev.lectern.diff.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public record Blocks(Map<BlockKey, Block> byKey) {

    public Blocks {
        byKey = Collections.unmodifiableMap(new TreeMap<>(byKey));
        Set<StateId> seen = new HashSet<>();
        for (Map.Entry<BlockKey, Block> entry : byKey.entrySet()) {
            if (!entry.getKey().equals(entry.getValue().key())) {
                throw new IllegalArgumentException(
                        "Block " + entry.getValue().key() + " is stored under " + entry.getKey());
            }
            for (StateId id : entry.getValue().states().values()) {
                if (!seen.add(id)) {
                    throw new IllegalArgumentException(
                            "State id " + id + " is used by more than one state, one of them in block " + entry.getKey());
                }
            }
        }
    }

    public int stateCount() {
        return byKey.values().stream().mapToInt(block -> block.states().size()).sum();
    }
}
