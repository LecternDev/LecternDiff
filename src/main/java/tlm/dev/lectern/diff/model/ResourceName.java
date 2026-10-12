package tlm.dev.lectern.diff.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record ResourceName(String value) implements Comparable<ResourceName> {

    private static final Pattern SHAPE = Pattern.compile("[a-z0-9_.-]+(/[a-z0-9_.-]+)*");

    public ResourceName {
        Objects.requireNonNull(value, "value");
        if (!SHAPE.matcher(value).matches() || hasDotSegment(value)) {
            throw new IllegalArgumentException("Invalid resource name: '" + value + "'");
        }
    }

    private static boolean hasDotSegment(String value) {
        for (String segment : value.split("/", -1)) {
            if (segment.equals(".") || segment.equals("..")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int compareTo(ResourceName other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
