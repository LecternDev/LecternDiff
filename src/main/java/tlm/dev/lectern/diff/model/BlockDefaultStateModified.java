package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record BlockDefaultStateModified(BlockKey block, StateProperties oldDefault, StateProperties newDefault)
        implements BlockChange {

    public BlockDefaultStateModified {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(oldDefault, "oldDefault");
        Objects.requireNonNull(newDefault, "newDefault");
        if (oldDefault.equals(newDefault)) {
            throw new IllegalArgumentException("The default state of block " + block + " did not change");
        }
    }
}
