package tlm.dev.lectern.diff.fixtures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import tlm.dev.lectern.diff.model.ArgumentNode;
import tlm.dev.lectern.diff.model.CommandNode;
import tlm.dev.lectern.diff.model.CommandNodeName;
import tlm.dev.lectern.diff.model.CommandPath;
import tlm.dev.lectern.diff.model.CommandTree;
import tlm.dev.lectern.diff.model.LiteralNode;
import tlm.dev.lectern.diff.model.ParserId;
import tlm.dev.lectern.diff.model.ParserProperties;
import tlm.dev.lectern.diff.model.ParserPropertyValue;
import tlm.dev.lectern.diff.model.PermissionRule;

public final class CommandFixtures {

    private CommandFixtures() {
    }

    public record Command(String name, NodeBuilder node) {
    }

    public static Command command(String name, NodeBuilder node) {
        return new Command(name, node);
    }

    public static CommandTree tree(Command... commands) {
        Map<CommandNodeName, CommandNode> byName = new TreeMap<>();
        for (Command command : commands) {
            byName.put(new CommandNodeName(command.name()), command.node().build());
        }
        return new CommandTree(byName);
    }

    public static NodeBuilder literal() {
        return new NodeBuilder(null);
    }

    public static NodeBuilder argument(String parser) {
        return new NodeBuilder(parser);
    }

    public static CommandNodeName name(String value) {
        return new CommandNodeName(value);
    }

    public static CommandPath path(String... names) {
        List<CommandNodeName> list = new ArrayList<>();
        for (String name : names) {
            list.add(new CommandNodeName(name));
        }
        return new CommandPath(list);
    }

    public static PermissionRule requires(String level) {
        return new PermissionRule("minecraft:require", "minecraft:command_level", level);
    }

    public static ParserProperties properties(Map<String, ParserPropertyValue> values) {
        return new ParserProperties(values);
    }

    public static ParserPropertyValue text(String value) {
        return new ParserPropertyValue.Text(value);
    }

    public static ParserPropertyValue number(String text) {
        return new ParserPropertyValue.Number(text);
    }

    public static final class NodeBuilder {

        private final String parser;
        private final Map<String, ParserPropertyValue> properties = new TreeMap<>();
        private final Map<CommandNodeName, NodeBuilder> children = new TreeMap<>();
        private boolean executable;
        private Optional<PermissionRule> permissions = Optional.empty();
        private Optional<CommandPath> redirect = Optional.empty();
        private boolean hasProperties;

        private NodeBuilder(String parser) {
            this.parser = parser;
        }

        public NodeBuilder executable() {
            this.executable = true;
            return this;
        }

        public NodeBuilder requires(String level) {
            this.permissions = Optional.of(CommandFixtures.requires(level));
            return this;
        }

        public NodeBuilder redirect(String... names) {
            this.redirect = Optional.of(path(names));
            return this;
        }

        public NodeBuilder text(String name, String value) {
            properties.put(name, new ParserPropertyValue.Text(value));
            hasProperties = true;
            return this;
        }

        public NodeBuilder number(String name, String text) {
            properties.put(name, new ParserPropertyValue.Number(text));
            hasProperties = true;
            return this;
        }

        public NodeBuilder child(String name, NodeBuilder node) {
            children.put(new CommandNodeName(name), node);
            return this;
        }

        public CommandNode build() {
            Map<CommandNodeName, CommandNode> built = new TreeMap<>();
            children.forEach((name, node) -> built.put(name, node.build()));
            if (parser == null) {
                return new LiteralNode(executable, permissions, redirect, built);
            }
            return new ArgumentNode(new ParserId(parser), hasProperties ? Optional.of(new ParserProperties(properties)) : Optional.empty(),
                    executable, permissions, redirect, built);
        }
    }
}
