package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record CommandAdded(CommandNodeName command, NodeKind kind, int nodeCount) implements CommandChange {

    public CommandAdded {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(kind, "kind");
        if (nodeCount < 1) {
            throw new IllegalArgumentException("A command has at least one node: " + nodeCount);
        }
    }
}
