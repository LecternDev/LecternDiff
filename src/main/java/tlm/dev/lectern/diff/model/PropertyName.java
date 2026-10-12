package tlm.dev.lectern.diff.model;

public record PropertyName(String value) implements Comparable<PropertyName> {

    public PropertyName {
        SnakeCaseSyntax.require(value, "property name");
    }

    @Override
    public int compareTo(PropertyName other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
