package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record PacketAdded(PacketPhase phase, PacketDirection direction, PacketName packet, ProtocolId id)
        implements PacketChange {

    public PacketAdded {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(packet, "packet");
        Objects.requireNonNull(id, "id");
    }
}
