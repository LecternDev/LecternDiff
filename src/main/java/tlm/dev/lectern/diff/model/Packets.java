package tlm.dev.lectern.diff.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public record Packets(Map<PacketGroup, Map<PacketName, ProtocolId>> groups) {

    public Packets {
        Map<PacketGroup, Map<PacketName, ProtocolId>> sorted = new TreeMap<>();
        groups.forEach((group, packets) -> sorted.put(group, Collections.unmodifiableMap(new TreeMap<>(packets))));
        groups = Collections.unmodifiableMap(sorted);
        for (Map.Entry<PacketGroup, Map<PacketName, ProtocolId>> group : groups.entrySet()) {
            Set<ProtocolId> seen = new HashSet<>();
            for (ProtocolId id : group.getValue().values()) {
                if (!seen.add(id)) {
                    throw new IllegalArgumentException("Protocol id " + id + " is used twice in " + group.getKey());
                }
            }
        }
    }

    public Set<PacketPhase> phases() {
        Set<PacketPhase> phases = new TreeSet<>();
        groups.keySet().forEach(group -> phases.add(group.phase()));
        return Collections.unmodifiableSet(phases);
    }

    public int packetCount(PacketPhase phase) {
        return groups.entrySet().stream()
                .filter(group -> group.getKey().phase().equals(phase))
                .mapToInt(group -> group.getValue().size())
                .sum();
    }
}
