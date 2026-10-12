package tlm.dev.lectern.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.direction;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.packet;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.phase;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.id;
import static tlm.dev.lectern.diff.fixtures.WorkbenchFixtures.requireReport;
import static tlm.dev.lectern.diff.fixtures.WorkbenchFixtures.tree;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.ids.IdShiftClassifier;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.model.PacketAdded;
import tlm.dev.lectern.diff.model.PacketIdModified;
import tlm.dev.lectern.diff.model.PacketIdsShifted;
import tlm.dev.lectern.diff.model.PacketRemoved;
import tlm.dev.lectern.diff.model.Packets;
import tlm.dev.lectern.diff.packets.PacketDiffer;
import tlm.dev.lectern.diff.read.PacketReportReader;
import tlm.dev.lectern.diff.read.VersionTree;

class WorkbenchPacketsDiffTest {

    private final PacketReportReader reader = new PacketReportReader(new JsonReader());
    private final PacketDiffer differ = new PacketDiffer(new IdShiftClassifier());

    private ChangeSet diff(String from, String to) {
        requireReport(PacketReportReader.FILE, from, to);
        VersionTree before = tree(from);
        VersionTree after = tree(to);
        List<Change> changes = List.copyOf(differ.diff(reader.read(before).orElseThrow(), reader.read(after).orElseThrow()));
        return new ChangeSet(before.version(), after.version(), changes);
    }

    private static <T extends Change> List<T> ofType(ChangeSet changeSet, Class<T> type) {
        return changeSet.changes().stream().filter(type::isInstance).map(type::cast).toList();
    }

    private static int packetCount(Packets packets) {
        return packets.groups().values().stream().mapToInt(Map::size).sum();
    }

    @Test
    void everyObservedGenerationThatHasTheReportIsReadable() {
        Map<String, Integer> expected = Map.of("1.21.11", 251, "26.3", 260, "26.3-rc-3", 260, "26.4-snapshot-3", 260);
        expected.keySet().forEach(version -> requireReport(PacketReportReader.FILE, version));
        expected.forEach((version, count) ->
                assertEquals(count, packetCount(reader.read(tree(version)).orElseThrow()), version));
    }

    @Test
    void releaseToSnapshotChangesNoPacket() {
        assertEquals(List.of(), diff("26.3", "26.4-snapshot-3").changes());
    }

    @Test
    void releaseCandidateToReleaseChangesNoPacket() {
        assertEquals(List.of(), diff("26.3-rc-3", "26.3").changes());
    }

    @Test
    void oneTwentyOneToTheNewNamingAddsTenPacketsInThreeGroups() {
        ChangeSet changeSet = diff("1.21.11", "26.3");
        Map<String, Set<String>> addedByGroup = ofType(changeSet, PacketAdded.class).stream().collect(Collectors.groupingBy(
                change -> change.phase() + "/" + change.direction().label(), TreeMap::new,
                Collectors.mapping(change -> change.packet().value(), Collectors.toCollection(TreeSet::new))));
        assertEquals(
                Map.of("configuration/clientbound", Set.of("minecraft:post_effects"),
                        "play/clientbound", Set.of("minecraft:add_transient_block", "minecraft:game_rule_values",
                                "minecraft:low_disk_space_warning", "minecraft:post_effects", "minecraft:swing_animation"),
                        "play/serverbound", Set.of("minecraft:attack", "minecraft:punch", "minecraft:set_game_rule",
                                "minecraft:spectator_action")),
                addedByGroup);
    }

    @Test
    void theRemovedPacketAndTheShiftedGroupsOfOneTwentyOneToTheNewNaming() {
        ChangeSet changeSet = diff("1.21.11", "26.3");
        List<PacketRemoved> removed = ofType(changeSet, PacketRemoved.class);
        assertEquals(1, removed.size());
        assertEquals(packet("minecraft:swing"), removed.get(0).packet());
        assertEquals(phase("play"), removed.get(0).phase());
        assertEquals(direction("serverbound"), removed.get(0).direction());
        assertEquals(
                List.of(new PacketIdsShifted(phase("configuration"), direction("clientbound"), 10, id(10)),
                        new PacketIdsShifted(phase("play"), direction("clientbound"), 102, id(37)),
                        new PacketIdsShifted(phase("play"), direction("serverbound"), 64, id(1))),
                ofType(changeSet, PacketIdsShifted.class));
        assertEquals(List.of(), ofType(changeSet, PacketIdModified.class));
        assertEquals(14, changeSet.changes().size());
    }
}
