package tlm.dev.lectern.diff.model;

public sealed interface PacketChange extends Change
        permits PacketPhaseAdded, PacketPhaseRemoved, PacketDirectionAdded, PacketDirectionRemoved, PacketAdded,
        PacketRemoved, PacketIdModified, PacketIdsShifted {
}
