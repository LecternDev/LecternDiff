package tlm.dev.lectern.diff.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public record ArgumentNode(
        ParserId parser,
        Optional<ParserProperties> properties,
        boolean executable,
        Optional<PermissionRule> permissions,
        Optional<CommandPath> redirect,
        Map<CommandNodeName, CommandNode> children) implements CommandNode {

    public ArgumentNode {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(properties, "properties");
        Objects.requireNonNull(permissions, "permissions");
        Objects.requireNonNull(redirect, "redirect");
        children = Collections.unmodifiableMap(new TreeMap<>(children));
    }

    @Override
    public NodeKind kind() {
        return NodeKind.ARGUMENT;
    }
}
