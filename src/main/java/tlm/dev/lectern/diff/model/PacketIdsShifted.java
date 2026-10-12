package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record PacketIdsShifted(PacketPhase phase, PacketDirection direction, int count, ProtocolId firstId)
        implements PacketChange {

    public PacketIdsShifted {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(firstId, "firstId");
        if (count < 1) {
            throw new IllegalArgumentException("A shift touches at least one packet: " + count);
        }
    }
}
