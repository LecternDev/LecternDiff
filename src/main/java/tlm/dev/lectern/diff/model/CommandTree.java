package tlm.dev.lectern.diff.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public record CommandTree(Map<CommandNodeName, CommandNode> commands) {

    public CommandTree {
        commands = Collections.unmodifiableMap(new TreeMap<>(commands));
        List<String> broken = new ArrayList<>();
        checkRedirects(commands, List.of(), broken);
        if (!broken.isEmpty()) {
            throw new IllegalArgumentException(broken.get(0));
        }
    }

    public Optional<CommandNode> node(CommandPath path) {
        return find(commands, path);
    }

    public int nodeCount() {
        return commands.values().stream().mapToInt(CommandNode::size).sum();
    }

    private static Optional<CommandNode> find(Map<CommandNodeName, CommandNode> roots, CommandPath path) {
        Map<CommandNodeName, CommandNode> level = roots;
        CommandNode found = null;
        for (CommandNodeName name : path.names()) {
            found = level.get(name);
            if (found == null) {
                return Optional.empty();
            }
            level = found.children();
        }
        return Optional.ofNullable(found);
    }

    private static void checkRedirects(
            Map<CommandNodeName, CommandNode> roots, List<CommandNodeName> prefix, List<String> broken) {
        Map<CommandNodeName, CommandNode> level = prefix.isEmpty() ? roots : find(roots, new CommandPath(prefix)).orElseThrow().children();
        for (Map.Entry<CommandNodeName, CommandNode> entry : level.entrySet()) {
            List<CommandNodeName> names = new ArrayList<>(prefix);
            names.add(entry.getKey());
            entry.getValue().redirect().ifPresent(target -> {
                if (find(roots, target).isEmpty()) {
                    broken.add("The redirect of node " + new CommandPath(names) + " points to " + target + ", which is not a node");
                }
            });
            checkRedirects(roots, names, broken);
        }
    }
}
