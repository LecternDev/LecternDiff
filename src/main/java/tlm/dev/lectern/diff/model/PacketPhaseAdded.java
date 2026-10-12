package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record PacketPhaseAdded(PacketPhase phase, int packetCount) implements PacketChange {

    public PacketPhaseAdded {
        Objects.requireNonNull(phase, "phase");
        if (packetCount < 0) {
            throw new IllegalArgumentException("Packet count cannot be negative: " + packetCount);
        }
    }
}
