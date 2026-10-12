package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record PacketDirectionRemoved(PacketPhase phase, PacketDirection direction, int packetCount)
        implements PacketChange {

    public PacketDirectionRemoved {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(direction, "direction");
        if (packetCount < 0) {
            throw new IllegalArgumentException("Packet count cannot be negative: " + packetCount);
        }
    }
}
