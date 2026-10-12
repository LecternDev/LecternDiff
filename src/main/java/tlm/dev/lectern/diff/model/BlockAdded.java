package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record BlockAdded(BlockKey block, int stateCount) implements BlockChange {

    public BlockAdded {
        Objects.requireNonNull(block, "block");
        if (stateCount < 1) {
            throw new IllegalArgumentException("A block has at least one state: " + stateCount);
        }
    }
}
