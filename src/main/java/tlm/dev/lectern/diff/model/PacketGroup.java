package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record PacketGroup(PacketPhase phase, PacketDirection direction) implements Comparable<PacketGroup> {

    public PacketGroup {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(direction, "direction");
    }

    @Override
    public int compareTo(PacketGroup other) {
        int byPhase = phase.compareTo(other.phase);
        return byPhase != 0 ? byPhase : direction.compareTo(other.direction);
    }

    @Override
    public String toString() {
        return phase + "/" + direction.label();
    }
}
