package tlm.dev.lectern.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tlm.dev.lectern.diff.fixtures.WorkbenchFixtures.requireReport;
import static tlm.dev.lectern.diff.fixtures.WorkbenchFixtures.tree;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.model.BlockChange;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeDomain;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.model.CommandChange;
import tlm.dev.lectern.diff.model.LootTableChange;
import tlm.dev.lectern.diff.model.PacketChange;
import tlm.dev.lectern.diff.model.RecipeChange;
import tlm.dev.lectern.diff.model.RegistryChange;
import tlm.dev.lectern.diff.model.TagChange;
import tlm.dev.lectern.diff.model.SkipReason;
import tlm.dev.lectern.diff.model.SkipSide;
import tlm.dev.lectern.diff.model.SkippedDomain;
import tlm.dev.lectern.diff.read.BlockReportReader;
import tlm.dev.lectern.diff.read.CommandReportReader;
import tlm.dev.lectern.diff.read.PacketReportReader;
import tlm.dev.lectern.diff.read.RegistryReportReader;

class WorkbenchDiffServiceTest {

    private final VersionDiffService service = Composition.diffService();

    private ChangeSet diff(String from, String to) {
        requireReport(RegistryReportReader.FILE, from, to);
        requireReport(BlockReportReader.FILE, from, to);
        requireReport(CommandReportReader.FILE, from, to);
        return service.diff(tree(from), tree(to));
    }

    private static String domainOf(Change change) {
        return switch (change) {
            case RegistryChange _ -> "registries";
            case BlockChange _ -> "blocks";
            case PacketChange _ -> "packets";
            case CommandChange _ -> "commands";
            case RecipeChange _ -> "recipes";
            case LootTableChange _ -> "loot_tables";
            case TagChange _ -> "tags";
        };
    }

    private static Map<String, Long> countsByDomain(ChangeSet changeSet) {
        return changeSet.changes().stream()
                .collect(Collectors.groupingBy(WorkbenchDiffServiceTest::domainOf, TreeMap::new, Collectors.counting()));
    }

    private static SkippedDomain skipped(ChangeDomain domain, SkipSide side) {
        return new SkippedDomain(domain, side, new SkipReason.ReportMissing());
    }

    @Test
    void releaseToSnapshotChangesRegistriesBlocksAndCommandsAndSkipsNothing() {
        requireReport(PacketReportReader.FILE, "26.3", "26.4-snapshot-3");
        ChangeSet changeSet = diff("26.3", "26.4-snapshot-3");
        assertEquals(List.of(), changeSet.skipped());
        assertEquals(Map.of("registries", 55L, "blocks", 3L, "commands", 3L), countsByDomain(changeSet));
        assertEquals(61, changeSet.changes().size());
    }

    @Test
    void releaseCandidateToReleaseIsAnEmptySetWithNothingSkipped() {
        requireReport(PacketReportReader.FILE, "26.3-rc-3", "26.3");
        ChangeSet changeSet = diff("26.3-rc-3", "26.3");
        assertEquals(List.of(), changeSet.changes());
        assertEquals(List.of(), changeSet.skipped());
    }

    @Test
    void oneTwentyFourToOneTwentyOneElevenSkipsThePacketsOnTheSideThatLacksThem() {
        ChangeSet changeSet = diff("1.20.4", "1.21.11");
        assertEquals(List.of(skipped(ChangeDomain.PACKETS, SkipSide.FROM)), changeSet.skipped());
        assertTrue(changeSet.changes().stream().noneMatch(change -> change instanceof PacketChange));
        Map<String, Long> counts = countsByDomain(changeSet);
        assertEquals(1365L, counts.get("registries"));
        assertEquals(346L, counts.get("commands"));
        assertTrue(counts.get("blocks") > 0);
    }

    @Test
    void oneFourteenFourToOneTwentyFourSkipsThePacketsOnBothSides() {
        ChangeSet changeSet = diff("1.14.4", "1.20.4");
        assertEquals(List.of(skipped(ChangeDomain.PACKETS, SkipSide.BOTH)), changeSet.skipped());
        Map<String, Long> counts = countsByDomain(changeSet);
        assertEquals(3536L, counts.get("registries"));
        assertEquals(154L, counts.get("commands"));
        assertTrue(counts.get("blocks") > 0);
        assertTrue(!counts.containsKey("packets"));
    }

    @Test
    void oneTwentyOneElevenToTheNewNamingChangesTheFourDomainsInTheOrderOfTheSchema() {
        requireReport(PacketReportReader.FILE, "1.21.11", "26.3");
        ChangeSet changeSet = diff("1.21.11", "26.3");
        assertEquals(List.of(), changeSet.skipped());
        Map<String, Long> counts = countsByDomain(changeSet);
        assertEquals(14L, counts.get("packets"));
        assertEquals(116L, counts.get("commands"));
        assertEquals(List.of("registries", "blocks", "packets", "commands"),
                changeSet.changes().stream().map(WorkbenchDiffServiceTest::domainOf).distinct().toList());
    }
}
