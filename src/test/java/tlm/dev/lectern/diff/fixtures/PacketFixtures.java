package tlm.dev.lectern.diff.fixtures;

import java.util.Map;
import java.util.TreeMap;
import tlm.dev.lectern.diff.model.PacketDirection;
import tlm.dev.lectern.diff.model.PacketGroup;
import tlm.dev.lectern.diff.model.PacketName;
import tlm.dev.lectern.diff.model.PacketPhase;
import tlm.dev.lectern.diff.model.Packets;
import tlm.dev.lectern.diff.model.ProtocolId;

public final class PacketFixtures {

    private PacketFixtures() {
    }

    public static PacketPhase phase(String value) {
        return new PacketPhase(value);
    }

    public static PacketDirection direction(String label) {
        return PacketDirection.ofLabel(label).orElseThrow();
    }

    public static PacketName packet(String name) {
        return new PacketName(name);
    }

    public static PacketGroup group(String phase, String direction) {
        return new PacketGroup(phase(phase), direction(direction));
    }

    public static GroupBuilder in(String phase, String direction) {
        return new GroupBuilder(group(phase, direction));
    }

    public static Packets packets(GroupBuilder... groups) {
        Map<PacketGroup, Map<PacketName, ProtocolId>> byGroup = new TreeMap<>();
        for (GroupBuilder group : groups) {
            byGroup.put(group.group, group.packets);
        }
        return new Packets(byGroup);
    }

    public static final class GroupBuilder {

        private final PacketGroup group;
        private final Map<PacketName, ProtocolId> packets = new TreeMap<>();

        private GroupBuilder(PacketGroup group) {
            this.group = group;
        }

        public GroupBuilder packet(String name, int id) {
            packets.put(new PacketName(name), new ProtocolId(id));
            return this;
        }

        public GroupBuilder contiguous(String... namesInIdOrder) {
            for (int index = 0; index < namesInIdOrder.length; index++) {
                packet(namesInIdOrder[index], index);
            }
            return this;
        }
    }
}
