package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record PacketIdModified(
        PacketPhase phase, PacketDirection direction, PacketName packet, ProtocolId oldId, ProtocolId newId)
        implements PacketChange {

    public PacketIdModified {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(packet, "packet");
        Objects.requireNonNull(oldId, "oldId");
        Objects.requireNonNull(newId, "newId");
        if (oldId.equals(newId)) {
            throw new IllegalArgumentException("The id of packet " + packet + " did not change");
        }
    }
}
