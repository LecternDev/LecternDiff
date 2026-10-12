package tlm.dev.lectern.diff.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record CommandPath(List<CommandNodeName> names) implements Comparable<CommandPath> {

    public CommandPath {
        names = List.copyOf(names);
        if (names.isEmpty()) {
            throw new IllegalArgumentException("A command path has at least one name");
        }
    }

    public static CommandPath of(CommandNodeName first) {
        return new CommandPath(List.of(first));
    }

    public CommandPath child(CommandNodeName name) {
        List<CommandNodeName> longer = new ArrayList<>(names);
        longer.add(name);
        return new CommandPath(longer);
    }

    @Override
    public int compareTo(CommandPath other) {
        int shared = Math.min(names.size(), other.names.size());
        for (int index = 0; index < shared; index++) {
            int byName = names.get(index).compareTo(other.names.get(index));
            if (byName != 0) {
                return byName;
            }
        }
        return Integer.compare(names.size(), other.names.size());
    }

    @Override
    public String toString() {
        return names.stream().map(CommandNodeName::value).collect(Collectors.joining(" > "));
    }
}
