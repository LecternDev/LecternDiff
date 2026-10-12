package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record BlockStateIdsModified(BlockKey block, int stateCount, StateId oldFirstId, StateId newFirstId)
        implements BlockChange {

    public BlockStateIdsModified {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(oldFirstId, "oldFirstId");
        Objects.requireNonNull(newFirstId, "newFirstId");
        if (stateCount < 1) {
            throw new IllegalArgumentException("A change touches at least one state: " + stateCount);
        }
    }
}
