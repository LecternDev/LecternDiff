package tlm.dev.lectern.diff.model;

public record PropertyValue(String value) implements Comparable<PropertyValue> {

    public PropertyValue {
        PropertySyntax.require(value, "property value");
    }

    @Override
    public int compareTo(PropertyValue other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
