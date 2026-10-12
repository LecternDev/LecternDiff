package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record CommandNodeAdded(CommandPath path, NodeKind kind, int nodeCount) implements CommandChange {

    public CommandNodeAdded {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(kind, "kind");
        if (path.names().size() < 2) {
            throw new IllegalArgumentException("A top level node is a command, not a node: " + path);
        }
        if (nodeCount < 1) {
            throw new IllegalArgumentException("A subtree has at least one node: " + nodeCount);
        }
    }
}
