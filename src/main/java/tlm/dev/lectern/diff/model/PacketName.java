package tlm.dev.lectern.diff.model;

public record PacketName(String value) implements Comparable<PacketName> {

    public PacketName {
        NamespacedName.require(value, "packet name");
    }

    @Override
    public int compareTo(PacketName other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
