package tlm.dev.lectern.diff.model;

import java.util.Map;
import java.util.Optional;

public sealed interface CommandNode permits LiteralNode, ArgumentNode {

    NodeKind kind();

    boolean executable();

    Optional<PermissionRule> permissions();

    Optional<CommandPath> redirect();

    Map<CommandNodeName, CommandNode> children();

    default int size() {
        return 1 + children().values().stream().mapToInt(CommandNode::size).sum();
    }
}
