package tlm.dev.lectern.diff.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record DataNamespace(String value) implements Comparable<DataNamespace> {

    private static final Pattern SHAPE = Pattern.compile("[a-z0-9_.-]+");

    public DataNamespace {
        Objects.requireNonNull(value, "value");
        if (!SHAPE.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid data namespace: '" + value + "'");
        }
    }

    @Override
    public int compareTo(DataNamespace other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
