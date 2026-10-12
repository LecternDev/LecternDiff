package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record BlockStateIdsShifted(int stateCount, int blockCount, StateId firstId) implements BlockChange {

    public BlockStateIdsShifted {
        Objects.requireNonNull(firstId, "firstId");
        if (stateCount < 1 || blockCount < 1 || blockCount > stateCount) {
            throw new IllegalArgumentException(
                    "A shift touches at least one state in at least one block: " + stateCount + " states, " + blockCount + " blocks");
        }
    }
}
