package tlm.dev.lectern.diff.read;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.PacketDirection;
import tlm.dev.lectern.diff.model.PacketGroup;
import tlm.dev.lectern.diff.model.PacketName;
import tlm.dev.lectern.diff.model.PacketPhase;
import tlm.dev.lectern.diff.model.Packets;
import tlm.dev.lectern.diff.model.ProtocolId;

public final class PacketReportReader {

    public static final String FILE = "reports/packets.json";

    private static final String PROTOCOL_ID = "protocol_id";
    private static final Set<String> PACKET_KEYS = Set.of(PROTOCOL_ID);
    private static final Set<String> DIRECTION_KEYS =
            Set.copyOf(PacketDirection.ALL.stream().map(PacketDirection::label).toList());

    private final JsonReader json;

    public PacketReportReader(JsonReader json) {
        this.json = json;
    }

    public Optional<Packets> read(VersionTree tree) {
        ReportContext report = new ReportContext(tree.version().id(), FILE);
        Optional<JsonValue> root = report.parse(tree, json);
        if (root.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Packets(readGroups(report, root.get())));
        } catch (IllegalArgumentException e) {
            throw report.failure(e.getMessage(), e);
        }
    }

    private Map<PacketGroup, Map<PacketName, ProtocolId>> readGroups(ReportContext report, JsonValue root) {
        JsonObject top = report.object(root, "the document");
        Map<PacketGroup, Map<PacketName, ProtocolId>> groups = new TreeMap<>();
        for (Map.Entry<String, JsonValue> member : top.members().entrySet()) {
            PacketPhase phase = new PacketPhase(member.getKey());
            String phaseWhere = "phase " + phase;
            JsonObject directions = report.object(member.getValue(), phaseWhere);
            if (directions.members().isEmpty()) {
                throw report.failure(phaseWhere + " has no direction");
            }
            report.rejectUnknownKeys(directions, DIRECTION_KEYS, phaseWhere);
            for (Map.Entry<String, JsonValue> direction : directions.members().entrySet()) {
                PacketGroup group = new PacketGroup(phase, PacketDirection.ofLabel(direction.getKey()).orElseThrow());
                groups.put(group, readPackets(report, group, report.object(direction.getValue(), "packets of " + group)));
            }
        }
        return groups;
    }

    private Map<PacketName, ProtocolId> readPackets(ReportContext report, PacketGroup group, JsonObject packets) {
        Map<PacketName, ProtocolId> read = new TreeMap<>();
        for (Map.Entry<String, JsonValue> member : packets.members().entrySet()) {
            String where = "packet " + member.getKey() + " of " + group;
            JsonObject packet = report.object(member.getValue(), where);
            report.rejectUnknownKeys(packet, PACKET_KEYS, where);
            read.put(new PacketName(member.getKey()), new ProtocolId(report.nonNegativeInt(packet, PROTOCOL_ID, where)));
        }
        return read;
    }
}
