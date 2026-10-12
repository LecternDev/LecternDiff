package tlm.dev.lectern.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.number;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.path;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.properties;
import static tlm.dev.lectern.diff.fixtures.WorkbenchFixtures.requireReport;
import static tlm.dev.lectern.diff.fixtures.WorkbenchFixtures.tree;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.commands.CommandDiffer;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.model.CommandAdded;
import tlm.dev.lectern.diff.model.CommandPermissionsModified;
import tlm.dev.lectern.diff.model.CommandPropertiesModified;
import tlm.dev.lectern.diff.model.CommandRemoved;
import tlm.dev.lectern.diff.model.CommandTree;
import tlm.dev.lectern.diff.model.ParserProperties;
import tlm.dev.lectern.diff.read.CommandReportReader;
import tlm.dev.lectern.diff.read.VersionTree;

class WorkbenchCommandsDiffTest {

    private final CommandReportReader reader = new CommandReportReader(new JsonReader());
    private final CommandDiffer differ = new CommandDiffer();

    private ChangeSet diff(String from, String to) {
        requireReport(CommandReportReader.FILE, from, to);
        VersionTree before = tree(from);
        VersionTree after = tree(to);
        List<Change> changes = List.copyOf(differ.diff(reader.read(before).orElseThrow(), reader.read(after).orElseThrow()));
        return new ChangeSet(before.version(), after.version(), changes);
    }

    private static <T extends Change> List<T> ofType(ChangeSet changeSet, Class<T> type) {
        return changeSet.changes().stream().filter(type::isInstance).map(type::cast).toList();
    }

    private static Map<String, Long> countsByType(ChangeSet changeSet) {
        return changeSet.changes().stream()
                .collect(Collectors.groupingBy(change -> change.getClass().getSimpleName(), TreeMap::new, Collectors.counting()));
    }

    private static Set<String> addedCommands(ChangeSet changeSet) {
        return ofType(changeSet, CommandAdded.class).stream().map(change -> change.command().value())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Test
    void everyObservedGenerationIsReadableWithTheNodeCountsOfTheReports() {
        Map<String, int[]> expected = Map.of(
                "1.14.4", new int[] {71, 1052},
                "1.20.4", new int[] {82, 1739},
                "1.21.11", new int[] {90, 2068},
                "26.3", new int[] {94, 2474},
                "26.3-rc-3", new int[] {94, 2474},
                "26.4-snapshot-3", new int[] {94, 2474});
        expected.keySet().forEach(version -> requireReport(CommandReportReader.FILE, version));
        expected.forEach((version, counts) -> {
            CommandTree commands = reader.read(tree(version)).orElseThrow();
            assertEquals(counts[0], commands.commands().size(), version + " top level commands");
            assertEquals(counts[1], commands.nodeCount(), version + " nodes");
        });
    }

    @Test
    void releaseToSnapshotOnlyGivesBoundsToThreeRotationStepsArguments() {
        Optional<ParserProperties> bounds =
                Optional.of(properties(Map.of("min", number("0"), "max", number("3"))));
        assertEquals(
                List.of(new CommandPropertiesModified(
                                path("test", "run", "tests", "numberOfTimes", "untilFailed", "rotationSteps"), Optional.empty(), bounds),
                        new CommandPropertiesModified(
                                path("test", "runfailed", "numberOfTimes", "untilFailed", "rotationSteps"), Optional.empty(), bounds),
                        new CommandPropertiesModified(
                                path("test", "runfailed", "onlyRequiredTests", "numberOfTimes", "untilFailed", "rotationSteps"),
                                Optional.empty(), bounds)),
                diff("26.3", "26.4-snapshot-3").changes());
    }

    @Test
    void releaseCandidateToReleaseChangesNoCommand() {
        assertEquals(List.of(), diff("26.3-rc-3", "26.3").changes());
    }

    @Test
    void oneTwentyOneElevenToTheNewNamingAddsFourCommandsAndNoPermission() {
        ChangeSet changeSet = diff("1.21.11", "26.3");
        assertEquals(Set.of("compute", "posteffect", "swing", "unpublish"), addedCommands(changeSet));
        assertEquals(List.of(), ofType(changeSet, CommandRemoved.class));
        assertEquals(List.of(), ofType(changeSet, CommandPermissionsModified.class));
        assertEquals(
                Map.of("CommandAdded", 4L, "CommandNodeAdded", 61L, "CommandNodeRemoved", 44L, "CommandParserModified", 4L,
                        "CommandPropertiesModified", 3L),
                countsByType(changeSet));
        assertEquals(116, changeSet.changes().size());
    }

    @Test
    void oneTwentyFourToOneTwentyOneElevenGivesPermissionsToSeventySixExistingNodes() {
        ChangeSet changeSet = diff("1.20.4", "1.21.11");
        assertEquals(Set.of("dialog", "fetchprofile", "rotate", "stopwatch", "test", "transfer", "version", "waypoint"),
                addedCommands(changeSet));
        List<CommandPermissionsModified> permissions = ofType(changeSet, CommandPermissionsModified.class);
        assertEquals(76, permissions.size());
        assertTrue(permissions.stream().allMatch(change -> change.oldPermissions().isEmpty() && change.newPermissions().isPresent()));
        assertEquals(Map.of("admins", 13L, "gamemasters", 56L, "owners", 7L),
                permissions.stream().collect(Collectors.groupingBy(change -> change.newPermissions().orElseThrow().level(),
                        TreeMap::new, Collectors.counting())));
        assertEquals(
                Map.of("CommandAdded", 8L, "CommandExecutableModified", 11L, "CommandNodeAdded", 145L, "CommandNodeRemoved", 58L,
                        "CommandParserModified", 35L, "CommandPermissionsModified", 76L, "CommandPropertiesModified", 13L),
                countsByType(changeSet));
        assertEquals(346, changeSet.changes().size());
    }

    @Test
    void oneFourteenFourToOneTwentyFourAddsTwelveCommandsAndRemovesOne() {
        ChangeSet changeSet = diff("1.14.4", "1.20.4");
        assertEquals(Set.of("attribute", "damage", "fillbiome", "item", "jfr", "perf", "place", "random", "return", "ride",
                "spectate", "tick"), addedCommands(changeSet));
        assertEquals(List.of("replaceitem"),
                ofType(changeSet, CommandRemoved.class).stream().map(change -> change.command().value()).toList());
        assertEquals(
                Map.of("CommandAdded", 12L, "CommandExecutableModified", 2L, "CommandNodeAdded", 88L, "CommandNodeRemoved", 25L,
                        "CommandParserModified", 13L, "CommandPropertiesModified", 13L, "CommandRemoved", 1L),
                countsByType(changeSet));
        assertEquals(154, changeSet.changes().size());
    }
}
