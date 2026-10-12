package tlm.dev.lectern.diff.model;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public record ResourceKind(String value) implements Comparable<ResourceKind> {

    private static final Pattern SHAPE = Pattern.compile("[a-z0-9_]+(/[a-z0-9_]+)*");

    public static final String TAGS_FOLDER = "tags";
    public static final ResourceKind RECIPE = new ResourceKind("recipe");
    public static final ResourceKind LOOT_TABLE = new ResourceKind("loot_table");

    public ResourceKind {
        Objects.requireNonNull(value, "value");
        if (!SHAPE.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid resource kind: '" + value + "'");
        }
    }

    public String folder() {
        int slash = value.indexOf('/');
        return slash < 0 ? value : value.substring(0, slash);
    }

    public Optional<String> subfolder() {
        int slash = value.indexOf('/');
        return slash < 0 ? Optional.empty() : Optional.of(value.substring(slash + 1));
    }

    @Override
    public int compareTo(ResourceKind other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
