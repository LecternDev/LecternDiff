package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record CommandNodeName(String value) implements Comparable<CommandNodeName> {

    public CommandNodeName {
        Objects.requireNonNull(value, "value");
        if (value.isEmpty() || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid command node name: '" + value + "'");
        }
    }

    @Override
    public int compareTo(CommandNodeName other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
