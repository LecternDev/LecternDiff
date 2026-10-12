package tlm.dev.lectern.diff.model;

public record EntryKey(String value) implements Comparable<EntryKey> {

    public EntryKey {
        NamespacedName.require(value, "entry key");
    }

    @Override
    public int compareTo(EntryKey other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
