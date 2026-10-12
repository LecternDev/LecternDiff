package tlm.dev.lectern.diff.model;

public record RegistryKey(String value) implements Comparable<RegistryKey> {

    public RegistryKey {
        NamespacedName.require(value, "registry key");
    }

    @Override
    public int compareTo(RegistryKey other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
