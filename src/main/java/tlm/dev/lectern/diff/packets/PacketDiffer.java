package tlm.dev.lectern.diff.packets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tlm.dev.lectern.diff.ids.IdShiftClassifier;
import tlm.dev.lectern.diff.model.PacketAdded;
import tlm.dev.lectern.diff.model.PacketChange;
import tlm.dev.lectern.diff.model.PacketDirectionAdded;
import tlm.dev.lectern.diff.model.PacketDirectionRemoved;
import tlm.dev.lectern.diff.model.PacketGroup;
import tlm.dev.lectern.diff.model.PacketIdModified;
import tlm.dev.lectern.diff.model.PacketIdsShifted;
import tlm.dev.lectern.diff.model.PacketName;
import tlm.dev.lectern.diff.model.PacketPhase;
import tlm.dev.lectern.diff.model.PacketPhaseAdded;
import tlm.dev.lectern.diff.model.PacketPhaseRemoved;
import tlm.dev.lectern.diff.model.PacketRemoved;
import tlm.dev.lectern.diff.model.Packets;
import tlm.dev.lectern.diff.model.ProtocolId;

public final class PacketDiffer {

    private final IdShiftClassifier classifier;

    public PacketDiffer(IdShiftClassifier classifier) {
        this.classifier = classifier;
    }

    public List<PacketChange> diff(Packets from, Packets to) {
        List<PacketChange> changes = new ArrayList<>();
        Set<PacketPhase> before = from.phases();
        Set<PacketPhase> after = to.phases();
        for (PacketPhase phase : before) {
            if (!after.contains(phase)) {
                changes.add(new PacketPhaseRemoved(phase, from.packetCount(phase)));
            }
        }
        for (PacketPhase phase : after) {
            if (!before.contains(phase)) {
                changes.add(new PacketPhaseAdded(phase, to.packetCount(phase)));
            }
        }
        for (Map.Entry<PacketGroup, Map<PacketName, ProtocolId>> group : from.groups().entrySet()) {
            PacketGroup key = group.getKey();
            if (!after.contains(key.phase())) {
                continue;
            }
            Map<PacketName, ProtocolId> now = to.groups().get(key);
            if (now == null) {
                changes.add(new PacketDirectionRemoved(key.phase(), key.direction(), group.getValue().size()));
            } else {
                diffGroup(key, group.getValue(), now, changes);
            }
        }
        for (Map.Entry<PacketGroup, Map<PacketName, ProtocolId>> group : to.groups().entrySet()) {
            PacketGroup key = group.getKey();
            if (before.contains(key.phase()) && !from.groups().containsKey(key)) {
                changes.add(new PacketDirectionAdded(key.phase(), key.direction(), group.getValue().size()));
            }
        }
        return List.copyOf(changes);
    }

    private void diffGroup(
            PacketGroup group, Map<PacketName, ProtocolId> was, Map<PacketName, ProtocolId> now, List<PacketChange> changes) {
        for (Map.Entry<PacketName, ProtocolId> packet : now.entrySet()) {
            if (!was.containsKey(packet.getKey())) {
                changes.add(new PacketAdded(group.phase(), group.direction(), packet.getKey(), packet.getValue()));
            }
        }
        for (Map.Entry<PacketName, ProtocolId> packet : was.entrySet()) {
            if (!now.containsKey(packet.getKey())) {
                changes.add(new PacketRemoved(group.phase(), group.direction(), packet.getKey(), packet.getValue()));
            }
        }
        IdShiftClassifier.Result<PacketName> ids = classifier.classify(values(was), values(now));
        ids.firstShiftedId().ifPresent(first -> changes.add(
                new PacketIdsShifted(group.phase(), group.direction(), ids.shiftedCount(), new ProtocolId(first))));
        for (IdShiftClassifier.Modification<PacketName> modification : ids.modified()) {
            changes.add(new PacketIdModified(group.phase(), group.direction(), modification.key(),
                    new ProtocolId(modification.oldId()), new ProtocolId(modification.newId())));
        }
    }

    private static Map<PacketName, Integer> values(Map<PacketName, ProtocolId> ids) {
        Map<PacketName, Integer> values = new HashMap<>();
        ids.forEach((name, id) -> values.put(name, id.value()));
        return values;
    }
}
