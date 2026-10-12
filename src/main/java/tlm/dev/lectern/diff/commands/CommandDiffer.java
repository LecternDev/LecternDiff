package tlm.dev.lectern.diff.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tlm.dev.lectern.diff.model.ArgumentNode;
import tlm.dev.lectern.diff.model.CommandAdded;
import tlm.dev.lectern.diff.model.CommandChange;
import tlm.dev.lectern.diff.model.CommandExecutableModified;
import tlm.dev.lectern.diff.model.CommandNode;
import tlm.dev.lectern.diff.model.CommandNodeAdded;
import tlm.dev.lectern.diff.model.CommandNodeName;
import tlm.dev.lectern.diff.model.CommandNodeRemoved;
import tlm.dev.lectern.diff.model.CommandParserModified;
import tlm.dev.lectern.diff.model.CommandPath;
import tlm.dev.lectern.diff.model.CommandPermissionsModified;
import tlm.dev.lectern.diff.model.CommandPropertiesModified;
import tlm.dev.lectern.diff.model.CommandRedirectModified;
import tlm.dev.lectern.diff.model.CommandRemoved;
import tlm.dev.lectern.diff.model.CommandTree;

public final class CommandDiffer {

    public List<CommandChange> diff(CommandTree from, CommandTree to) {
        List<CommandChange> changes = new ArrayList<>();
        for (Map.Entry<CommandNodeName, CommandNode> command : from.commands().entrySet()) {
            CommandNode now = to.commands().get(command.getKey());
            if (now == null || now.kind() != command.getValue().kind()) {
                changes.add(new CommandRemoved(command.getKey(), command.getValue().kind(), command.getValue().size()));
            } else {
                compareNodes(CommandPath.of(command.getKey()), command.getValue(), now, changes);
            }
        }
        for (Map.Entry<CommandNodeName, CommandNode> command : to.commands().entrySet()) {
            CommandNode was = from.commands().get(command.getKey());
            if (was == null || was.kind() != command.getValue().kind()) {
                changes.add(new CommandAdded(command.getKey(), command.getValue().kind(), command.getValue().size()));
            }
        }
        return List.copyOf(changes);
    }

    private void compareNodes(CommandPath path, CommandNode before, CommandNode after, List<CommandChange> changes) {
        if (before instanceof ArgumentNode was && after instanceof ArgumentNode now) {
            if (!was.parser().equals(now.parser())) {
                changes.add(new CommandParserModified(path, was.parser(), now.parser()));
            }
            if (!was.properties().equals(now.properties())) {
                changes.add(new CommandPropertiesModified(path, was.properties(), now.properties()));
            }
        }
        if (before.executable() != after.executable()) {
            changes.add(new CommandExecutableModified(path, before.executable(), after.executable()));
        }
        if (!before.permissions().equals(after.permissions())) {
            changes.add(new CommandPermissionsModified(path, before.permissions(), after.permissions()));
        }
        if (!before.redirect().equals(after.redirect())) {
            changes.add(new CommandRedirectModified(path, before.redirect(), after.redirect()));
        }
        compareChildren(path, before.children(), after.children(), changes);
    }

    private void compareChildren(
            CommandPath parent,
            Map<CommandNodeName, CommandNode> before,
            Map<CommandNodeName, CommandNode> after,
            List<CommandChange> changes) {
        for (Map.Entry<CommandNodeName, CommandNode> child : before.entrySet()) {
            CommandPath path = parent.child(child.getKey());
            CommandNode now = after.get(child.getKey());
            if (now == null || now.kind() != child.getValue().kind()) {
                changes.add(new CommandNodeRemoved(path, child.getValue().kind(), child.getValue().size()));
            } else {
                compareNodes(path, child.getValue(), now, changes);
            }
        }
        for (Map.Entry<CommandNodeName, CommandNode> child : after.entrySet()) {
            CommandNode was = before.get(child.getKey());
            if (was == null || was.kind() != child.getValue().kind()) {
                changes.add(new CommandNodeAdded(parent.child(child.getKey()), child.getValue().kind(), child.getValue().size()));
            }
        }
    }
}
