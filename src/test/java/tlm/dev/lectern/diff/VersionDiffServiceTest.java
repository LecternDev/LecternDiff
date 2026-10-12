package tlm.dev.lectern.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.blockKey;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.property;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.stateId;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.value;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.values;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.direction;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.packet;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.phase;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.entry;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.id;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.key;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.fixtures.GadgetScenario;
import tlm.dev.lectern.diff.fixtures.InMemoryVersionTree;
import tlm.dev.lectern.diff.model.BlockAdded;
import tlm.dev.lectern.diff.model.BlockChange;
import tlm.dev.lectern.diff.model.BlockPropertyAdded;
import tlm.dev.lectern.diff.model.BlockStateIdsShifted;
import tlm.dev.lectern.diff.model.ChangeDomain;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.model.PacketAdded;
import tlm.dev.lectern.diff.model.PacketChange;
import tlm.dev.lectern.diff.model.PacketIdModified;
import tlm.dev.lectern.diff.model.PacketIdsShifted;
import tlm.dev.lectern.diff.model.PacketPhaseAdded;
import tlm.dev.lectern.diff.model.PacketPhaseRemoved;
import tlm.dev.lectern.diff.model.RegistryAdded;
import tlm.dev.lectern.diff.model.RegistryChange;
import tlm.dev.lectern.diff.model.RegistryEntryAdded;
import tlm.dev.lectern.diff.model.RegistryEntryIdsShifted;
import tlm.dev.lectern.diff.model.RegistryRemoved;
import tlm.dev.lectern.diff.model.SkipReason;
import tlm.dev.lectern.diff.model.SkipSide;
import tlm.dev.lectern.diff.model.SkippedDomain;
import tlm.dev.lectern.diff.read.VersionReadException;
import tlm.dev.lectern.diff.read.VersionTree;

class VersionDiffServiceTest {

    private static final String REGISTRIES = "reports/registries.json";
    private static final String BLOCKS = "reports/blocks.json";
    private static final String PACKETS = "reports/packets.json";

    private final VersionDiffService service = Composition.diffService();

    private static VersionTree tree(String versionId, Map<String, String> reports) {
        return new InMemoryVersionTree(release(versionId), reports);
    }

    private static VersionTree before() {
        return tree("1.0", Map.of(REGISTRIES, GadgetScenario.BEFORE, BLOCKS, GadgetScenario.BLOCKS_BEFORE,
                PACKETS, GadgetScenario.PACKETS_BEFORE));
    }

    private static VersionTree after() {
        return tree("1.1", Map.of(REGISTRIES, GadgetScenario.AFTER, BLOCKS, GadgetScenario.BLOCKS_AFTER,
                PACKETS, GadgetScenario.PACKETS_AFTER));
    }

    private static VersionTree without(VersionTree complete, String report) {
        Map<String, String> files = new HashMap<>();
        for (String file : List.of(REGISTRIES, BLOCKS, PACKETS)) {
            if (!file.equals(report)) {
                files.put(file, complete.readText(file).orElseThrow());
            }
        }
        return new InMemoryVersionTree(complete.version(), files);
    }

    private static SkippedDomain skipped(ChangeDomain domain, SkipSide side) {
        return new SkippedDomain(domain, side, new SkipReason.ReportMissing());
    }

    @Test
    void diffsTwoVersionTreesFromTheirReportsInOneSortedChangeSet() {
        ChangeSet changeSet = service.diff(before(), after());
        assertEquals(release("1.0"), changeSet.from());
        assertEquals(release("1.1"), changeSet.to());
        assertEquals(List.of(), changeSet.skipped());
        assertEquals(
                List.of(new RegistryEntryAdded(key("demo:gadget"), entry("demo:glossy"), id(1)),
                        new RegistryEntryIdsShifted(key("demo:gadget"), 2, id(1)),
                        new RegistryRemoved(key("demo:gizmo"), id(1), 1),
                        new RegistryAdded(key("demo:widget"), id(1), 1),
                        new BlockStateIdsShifted(3, 2, stateId(0)),
                        new BlockAdded(blockKey("demo:anvil"), 1),
                        new BlockPropertyAdded(blockKey("demo:lamp"), property("dim"), values("low", "high"), value("low")),
                        new PacketPhaseAdded(phase("login"), 1),
                        new PacketAdded(phase("play"), direction("clientbound"), packet("demo:flash"), id(1)),
                        new PacketIdModified(phase("play"), direction("serverbound"), packet("demo:chat"), id(0), id(1)),
                        new PacketIdModified(phase("play"), direction("serverbound"), packet("demo:jump"), id(1), id(0)),
                        new PacketIdsShifted(phase("play"), direction("clientbound"), 2, id(1)),
                        new PacketPhaseRemoved(phase("status"), 1)),
                changeSet.changes());
    }

    @Test
    void comparingAVersionWithItselfGivesNoChangeAndNothingSkipped() {
        ChangeSet changeSet = service.diff(before(), before());
        assertEquals(List.of(), changeSet.changes());
        assertEquals(List.of(), changeSet.skipped());
    }

    @Test
    void theChangesOfTheDomainsAreInTheOrderOfTheSchema() {
        List<String> domains = service.diff(before(), after()).changes().stream()
                .map(change -> switch (change) {
                    case RegistryChange _ -> "registries";
                    case BlockChange _ -> "blocks";
                    case PacketChange _ -> "packets";
                })
                .distinct()
                .toList();
        assertEquals(List.of("registries", "blocks", "packets"), domains);
    }

    @Test
    void aReportMissingFromTheFirstVersionSkipsThatDomainAndKeepsTheOthers() {
        ChangeSet changeSet = service.diff(without(before(), BLOCKS), after());
        assertEquals(List.of(skipped(ChangeDomain.BLOCKS, SkipSide.FROM)), changeSet.skipped());
        assertEquals(10, changeSet.changes().size());
        assertTrue(changeSet.changes().stream().noneMatch(change -> change instanceof BlockChange));
    }

    @Test
    void aReportMissingFromTheSecondVersionSkipsThatDomainAndKeepsTheOthers() {
        ChangeSet changeSet = service.diff(before(), without(after(), REGISTRIES));
        assertEquals(List.of(skipped(ChangeDomain.REGISTRIES, SkipSide.TO)), changeSet.skipped());
        assertEquals(9, changeSet.changes().size());
        assertTrue(changeSet.changes().stream().noneMatch(change -> change instanceof RegistryChange));
    }

    @Test
    void aReportMissingFromBothVersionsIsSkippedWithTheSideBoth() {
        ChangeSet changeSet = service.diff(without(before(), BLOCKS), without(after(), BLOCKS));
        assertEquals(List.of(skipped(ChangeDomain.BLOCKS, SkipSide.BOTH)), changeSet.skipped());
        assertEquals(10, changeSet.changes().size());
    }

    @Test
    void aPacketReportThatDoesNotExistYetSkipsThePacketsDomainOnTheSideThatLacksIt() {
        ChangeSet changeSet = service.diff(without(before(), PACKETS), after());
        assertEquals(List.of(skipped(ChangeDomain.PACKETS, SkipSide.FROM)), changeSet.skipped());
        assertTrue(changeSet.changes().stream().noneMatch(change -> change instanceof PacketChange));
        ChangeSet both = service.diff(without(before(), PACKETS), without(after(), PACKETS));
        assertEquals(List.of(skipped(ChangeDomain.PACKETS, SkipSide.BOTH)), both.skipped());
    }

    @Test
    void aVersionWithoutAnyReportSkipsEveryDomainInTheOrderOfTheSchema() {
        ChangeSet changeSet = service.diff(tree("1.0", Map.of()), tree("1.1", Map.of()));
        assertEquals(
                List.of(skipped(ChangeDomain.REGISTRIES, SkipSide.BOTH), skipped(ChangeDomain.BLOCKS, SkipSide.BOTH),
                        skipped(ChangeDomain.PACKETS, SkipSide.BOTH)),
                changeSet.skipped());
        assertEquals(List.of(), changeSet.changes());
    }

    @Test
    void aMissingReportIsNeverReadAsEverythingAddedOrRemoved() {
        ChangeSet changeSet = service.diff(tree("1.0", Map.of()), after());
        assertEquals(
                List.of(skipped(ChangeDomain.REGISTRIES, SkipSide.FROM), skipped(ChangeDomain.BLOCKS, SkipSide.FROM),
                        skipped(ChangeDomain.PACKETS, SkipSide.FROM)),
                changeSet.skipped());
        assertEquals(List.of(), changeSet.changes());
    }

    @Test
    void anIllegibleRegistriesReportStopsTheDiffAndNamesThatVersion() {
        VersionTree broken = tree("1.1", Map.of(REGISTRIES, "{\"demo:gadget\": []}", BLOCKS, GadgetScenario.BLOCKS_AFTER,
                PACKETS, GadgetScenario.PACKETS_AFTER));
        VersionReadException failure = assertThrows(VersionReadException.class, () -> service.diff(before(), broken));
        assertEquals("Version 1.1, reports/registries.json: registry demo:gadget is not an object", failure.getMessage());
    }

    @Test
    void anIllegibleBlocksReportStopsTheDiffAndNamesThatVersion() {
        VersionTree broken = tree("1.1", Map.of(REGISTRIES, GadgetScenario.AFTER, BLOCKS, "{\"demo:crate\": []}",
                PACKETS, GadgetScenario.PACKETS_AFTER));
        VersionReadException failure = assertThrows(VersionReadException.class, () -> service.diff(before(), broken));
        assertEquals("Version 1.1, reports/blocks.json: block demo:crate is not an object", failure.getMessage());
    }

    @Test
    void anIllegiblePacketsReportStopsTheDiffAndNamesThatVersion() {
        VersionTree broken = tree("1.1", Map.of(REGISTRIES, GadgetScenario.AFTER, BLOCKS, GadgetScenario.BLOCKS_AFTER,
                PACKETS, "{\"play\": []}"));
        VersionReadException failure = assertThrows(VersionReadException.class, () -> service.diff(before(), broken));
        assertEquals("Version 1.1, reports/packets.json: phase play is not an object", failure.getMessage());
    }

    @Test
    void anIllegibleReportStaysAStrongErrorEvenWhenTheOtherVersionHasNone() {
        VersionTree broken = tree("1.1", Map.of(BLOCKS, "{\"demo:crate\": []}"));
        VersionReadException failure = assertThrows(VersionReadException.class, () -> service.diff(tree("1.0", Map.of()), broken));
        assertEquals("Version 1.1, reports/blocks.json: block demo:crate is not an object", failure.getMessage());
    }
}
