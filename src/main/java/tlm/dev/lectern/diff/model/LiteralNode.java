package tlm.dev.lectern.diff.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public record LiteralNode(
        boolean executable,
        Optional<PermissionRule> permissions,
        Optional<CommandPath> redirect,
        Map<CommandNodeName, CommandNode> children) implements CommandNode {

    public LiteralNode {
        Objects.requireNonNull(permissions, "permissions");
        Objects.requireNonNull(redirect, "redirect");
        children = Collections.unmodifiableMap(new TreeMap<>(children));
    }

    @Override
    public NodeKind kind() {
        return NodeKind.LITERAL;
    }
}
