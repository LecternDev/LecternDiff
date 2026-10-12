package tlm.dev.lectern.diff.model;

public record BlockKey(String value) implements Comparable<BlockKey> {

    public BlockKey {
        NamespacedName.require(value, "block key");
    }

    @Override
    public int compareTo(BlockKey other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
