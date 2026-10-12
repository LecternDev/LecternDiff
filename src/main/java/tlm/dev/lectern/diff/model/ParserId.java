package tlm.dev.lectern.diff.model;

public record ParserId(String value) implements Comparable<ParserId> {

    public ParserId {
        NamespacedName.require(value, "parser id");
    }

    @Override
    public int compareTo(ParserId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
