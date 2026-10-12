package tlm.dev.lectern.diff.model;

public record PacketPhase(String value) implements Comparable<PacketPhase> {

    public PacketPhase {
        SnakeCaseSyntax.require(value, "packet phase");
    }

    @Override
    public int compareTo(PacketPhase other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
