package tlm.dev.lectern.diff.write;

import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.model.PacketAdded;
import tlm.dev.lectern.diff.model.PacketChange;
import tlm.dev.lectern.diff.model.PacketDirectionAdded;
import tlm.dev.lectern.diff.model.PacketDirectionRemoved;
import tlm.dev.lectern.diff.model.PacketIdModified;
import tlm.dev.lectern.diff.model.PacketIdsShifted;
import tlm.dev.lectern.diff.model.PacketPhaseAdded;
import tlm.dev.lectern.diff.model.PacketPhaseRemoved;
import tlm.dev.lectern.diff.model.PacketRemoved;

public final class PacketChangeRenderer {

    public JsonObject render(PacketChange change) {
        return switch (change) {
            case PacketPhaseAdded added -> JsonObject.builder()
                    .put("type", "packet.phase_added")
                    .put("phase", added.phase().value())
                    .put("packet_count", added.packetCount())
                    .build();
            case PacketPhaseRemoved removed -> JsonObject.builder()
                    .put("type", "packet.phase_removed")
                    .put("phase", removed.phase().value())
                    .put("packet_count", removed.packetCount())
                    .build();
            case PacketDirectionAdded added -> JsonObject.builder()
                    .put("type", "packet.direction_added")
                    .put("phase", added.phase().value())
                    .put("direction", added.direction().label())
                    .put("packet_count", added.packetCount())
                    .build();
            case PacketDirectionRemoved removed -> JsonObject.builder()
                    .put("type", "packet.direction_removed")
                    .put("phase", removed.phase().value())
                    .put("direction", removed.direction().label())
                    .put("packet_count", removed.packetCount())
                    .build();
            case PacketAdded added -> JsonObject.builder()
                    .put("type", "packet.added")
                    .put("phase", added.phase().value())
                    .put("direction", added.direction().label())
                    .put("packet", added.packet().value())
                    .put("protocol_id", added.id().value())
                    .build();
            case PacketRemoved removed -> JsonObject.builder()
                    .put("type", "packet.removed")
                    .put("phase", removed.phase().value())
                    .put("direction", removed.direction().label())
                    .put("packet", removed.packet().value())
                    .put("protocol_id", removed.id().value())
                    .build();
            case PacketIdModified modified -> JsonObject.builder()
                    .put("type", "packet.id_modified")
                    .put("phase", modified.phase().value())
                    .put("direction", modified.direction().label())
                    .put("packet", modified.packet().value())
                    .put("old_id", modified.oldId().value())
                    .put("new_id", modified.newId().value())
                    .build();
            case PacketIdsShifted shifted -> JsonObject.builder()
                    .put("type", "packet.ids_shifted")
                    .put("phase", shifted.phase().value())
                    .put("direction", shifted.direction().label())
                    .put("count", shifted.count())
                    .put("first_id", shifted.firstId().value())
                    .build();
        };
    }
}
