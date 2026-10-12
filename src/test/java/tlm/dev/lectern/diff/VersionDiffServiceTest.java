package tlm.dev.lectern.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.blockKey;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.property;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.stateId;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.value;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.values;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.name;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.number;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.path;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.properties;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.requires;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.json;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.lootTable;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.recipe;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.resource;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.tag;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.direction;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.packet;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.phase;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.entry;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.id;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.key;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.fixtures.GadgetScenario;
import tlm.dev.lectern.diff.fixtures.InMemoryVersionTree;
import tlm.dev.lectern.diff.jsondiff.JsonPointer;
import tlm.dev.lectern.diff.jsondiff.PlaceChange;
import tlm.dev.lectern.diff.model.BlockAdded;
import tlm.dev.lectern.diff.model.BlockChange;
import tlm.dev.lectern.diff.model.BlockPropertyAdded;
import tlm.dev.lectern.diff.model.BlockStateIdsShifted;
import tlm.dev.lectern.diff.model.ChangeDomain;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.model.CommandAdded;
import tlm.dev.lectern.diff.model.CommandChange;
import tlm.dev.lectern.diff.model.CommandExecutableModified;
import tlm.dev.lectern.diff.model.CommandNodeAdded;
import tlm.dev.lectern.diff.model.CommandPermissionsModified;
import tlm.dev.lectern.diff.model.CommandPropertiesModified;
import tlm.dev.lectern.diff.model.CommandRemoved;
import tlm.dev.lectern.diff.model.NodeKind;
import tlm.dev.lectern.diff.model.PacketAdded;
import tlm.dev.lectern.diff.model.LootTableAdded;
import tlm.dev.lectern.diff.model.LootTableChange;
import tlm.dev.lectern.diff.model.LootTableModified;
import tlm.dev.lectern.diff.model.LootTableRemoved;
import tlm.dev.lectern.diff.model.PacketChange;
import tlm.dev.lectern.diff.model.RecipeAdded;
import tlm.dev.lectern.diff.model.RecipeChange;
import tlm.dev.lectern.diff.model.RecipeModified;
import tlm.dev.lectern.diff.model.RecipeRemoved;
import tlm.dev.lectern.diff.model.PacketIdModified;
import tlm.dev.lectern.diff.model.PacketIdsShifted;
import tlm.dev.lectern.diff.model.PacketPhaseAdded;
import tlm.dev.lectern.diff.model.PacketPhaseRemoved;
import tlm.dev.lectern.diff.model.RegistryAdded;
import tlm.dev.lectern.diff.model.RegistryChange;
import tlm.dev.lectern.diff.model.TagAdded;
import tlm.dev.lectern.diff.model.TagChange;
import tlm.dev.lectern.diff.model.TagRemoved;
import tlm.dev.lectern.diff.model.TagValuesAdded;
import tlm.dev.lectern.diff.model.TagValuesRemoved;
import tlm.dev.lectern.diff.model.RegistryEntryAdded;
import tlm.dev.lectern.diff.model.RegistryEntryIdsShifted;
import tlm.dev.lectern.diff.model.RegistryRemoved;
import tlm.dev.lectern.diff.model.SkipReason;
import tlm.dev.lectern.diff.model.SkipSide;
import tlm.dev.lectern.diff.model.Skipped;
import tlm.dev.lectern.diff.model.SkippedDomain;
import tlm.dev.lectern.diff.model.SkippedFolder;
import tlm.dev.lectern.diff.read.VersionReadException;
import tlm.dev.lectern.diff.read.VersionTree;

class VersionDiffServiceTest {

    private static final String REGISTRIES = "reports/registries.json";
    private static final String BLOCKS = "reports/blocks.json";
    private static final String PACKETS = "reports/packets.json";
    private static final String COMMANDS = "reports/commands.json";

    private final VersionDiffService service = Composition.diffService();

    private static VersionTree tree(String versionId, Map<String, String> reports) {
        return new InMemoryVersionTree(release(versionId), reports);
    }

    private static VersionTree before() {
        return tree("1.0", GadgetScenario.beforeFiles());
    }

    private static VersionTree after() {
        return tree("1.1", GadgetScenario.afterFiles());
    }

    private static VersionTree without(VersionTree complete, String report) {
        Map<String, String> files = new HashMap<>();
        for (String file : allFiles(complete)) {
            if (!file.equals(report)) {
                complete.readText(file).ifPresent(text -> files.put(file, text));
            }
        }
        return new InMemoryVersionTree(complete.version(), files);
    }

    private static final List<SkippedFolder> FOLDERS = List.of(
            new SkippedFolder("datapacks", 0, 0, 1), new SkippedFolder("dialog", 1, 0, 0), new SkippedFolder("worldgen", 1, 0, 1));

    private static List<Skipped> withFolders(Skipped... domains) {
        List<Skipped> all = new ArrayList<>(List.of(domains));
        all.addAll(FOLDERS);
        return all;
    }

    private static List<Skipped> dataSkipped(SkipSide side) {
        return List.of(skipped(ChangeDomain.RECIPES, side), skipped(ChangeDomain.LOOT_TABLES, side), skipped(ChangeDomain.TAGS, side));
    }

    private static VersionTree with(VersionTree complete, String path, String text) {
        Map<String, String> files = new HashMap<>();
        for (String file : allFiles(complete)) {
            complete.readText(file).ifPresent(content -> files.put(file, content));
        }
        files.put(path, text);
        return new InMemoryVersionTree(complete.version(), files);
    }

    private static VersionTree onlyReports(VersionTree complete) {
        Map<String, String> files = new HashMap<>();
        for (String file : complete.files("reports")) {
            complete.readText(file).ifPresent(text -> files.put(file, text));
        }
        return new InMemoryVersionTree(complete.version(), files);
    }

    private static List<String> allFiles(VersionTree tree) {
        List<String> all = new ArrayList<>(tree.files("reports"));
        all.addAll(tree.files("data"));
        return all;
    }


    private static SkippedDomain skipped(ChangeDomain domain, SkipSide side) {
        return new SkippedDomain(domain, side, new SkipReason.ReportMissing());
    }

    @Test
    void diffsTwoVersionTreesFromTheirReportsInOneSortedChangeSet() {
        ChangeSet changeSet = service.diff(before(), after());
        assertEquals(release("1.0"), changeSet.from());
        assertEquals(release("1.1"), changeSet.to());
        assertEquals(FOLDERS, changeSet.skipped());
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
                        new PacketPhaseRemoved(phase("status"), 1),
                        new CommandAdded(name("dial"), NodeKind.LITERAL, 3),
                        new CommandPermissionsModified(path("gadget", "mode"), Optional.empty(), Optional.of(requires("gamemasters"))),
                        new CommandPropertiesModified(path("gadget", "size"),
                                Optional.of(properties(Map.of("min", number("0"), "max", number("3")))),
                                Optional.of(properties(Map.of("min", number("0"), "max", number("9"))))),
                        new CommandNodeAdded(path("gadget", "tint"), NodeKind.ARGUMENT, 2),
                        new CommandExecutableModified(path("lamp"), true, false),
                        new CommandRemoved(name("legacy"), NodeKind.LITERAL, 2),
                        new RecipeAdded(resource("demo", "recipe", "brewing/new_potion"), "demo:brewing", Optional.of("demo:potion")),
                        new RecipeModified(recipe("gadget"), List.of(
                                new PlaceChange.Modified(JsonPointer.root().child("ingredients").child(1), json("\"demo:b\""), json("\"demo:c\"")),
                                new PlaceChange.Modified(JsonPointer.root().child("result").child("count"), json("1"), json("2")))),
                        new RecipeRemoved(recipe("old_stew"), "demo:smelting", Optional.of("demo:stew")),
                        new LootTableModified(lootTable("blocks/crate"), List.of(
                                new PlaceChange.Modified(JsonPointer.root().child("pools").child(0).child("rolls"), json("1"), json("2")),
                                new PlaceChange.Added(JsonPointer.root().child("pools").child(1), json("{\"rolls\": 1, \"entries\": []}")))),
                        new LootTableRemoved(lootTable("blocks/vase"), Optional.empty()),
                        new LootTableAdded(lootTable("chests/vault"), Optional.of("demo:chest")),
                        new TagValuesAdded(tag("block", "logs"), List.of("demo:cherry_log")),
                        new TagValuesRemoved(tag("block", "logs"), List.of("demo:birch_log")),
                        new TagAdded(tag("block", "mineable/spade"), 2),
                        new TagRemoved(tag("item", "old_tools"), 2)),
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
                    case CommandChange _ -> "commands";
                    case RecipeChange _ -> "recipes";
                    case LootTableChange _ -> "loot_tables";
                    case TagChange _ -> "tags";
                })
                .distinct()
                .toList();
        assertEquals(List.of("registries", "blocks", "packets", "commands", "recipes", "loot_tables", "tags"), domains);
    }

    @Test
    void aReportMissingFromTheFirstVersionSkipsThatDomainAndKeepsTheOthers() {
        ChangeSet changeSet = service.diff(without(before(), BLOCKS), after());
        assertEquals(withFolders(skipped(ChangeDomain.BLOCKS, SkipSide.FROM)), changeSet.skipped());
        assertEquals(26, changeSet.changes().size());
        assertTrue(changeSet.changes().stream().noneMatch(change -> change instanceof BlockChange));
    }

    @Test
    void aReportMissingFromTheSecondVersionSkipsThatDomainAndKeepsTheOthers() {
        ChangeSet changeSet = service.diff(before(), without(after(), REGISTRIES));
        assertEquals(withFolders(skipped(ChangeDomain.REGISTRIES, SkipSide.TO)), changeSet.skipped());
        assertEquals(25, changeSet.changes().size());
        assertTrue(changeSet.changes().stream().noneMatch(change -> change instanceof RegistryChange));
    }

    @Test
    void aReportMissingFromBothVersionsIsSkippedWithTheSideBoth() {
        ChangeSet changeSet = service.diff(without(before(), BLOCKS), without(after(), BLOCKS));
        assertEquals(withFolders(skipped(ChangeDomain.BLOCKS, SkipSide.BOTH)), changeSet.skipped());
        assertEquals(26, changeSet.changes().size());
    }

    @Test
    void aPacketReportThatDoesNotExistYetSkipsThePacketsDomainOnTheSideThatLacksIt() {
        ChangeSet changeSet = service.diff(without(before(), PACKETS), after());
        assertEquals(withFolders(skipped(ChangeDomain.PACKETS, SkipSide.FROM)), changeSet.skipped());
        assertTrue(changeSet.changes().stream().noneMatch(change -> change instanceof PacketChange));
        ChangeSet both = service.diff(without(before(), PACKETS), without(after(), PACKETS));
        assertEquals(withFolders(skipped(ChangeDomain.PACKETS, SkipSide.BOTH)), both.skipped());
    }

    @Test
    void aCommandReportMissingFromOneVersionSkipsTheCommandsDomainOnThatSide() {
        ChangeSet changeSet = service.diff(before(), without(after(), COMMANDS));
        assertEquals(withFolders(skipped(ChangeDomain.COMMANDS, SkipSide.TO)), changeSet.skipped());
        assertEquals(23, changeSet.changes().size());
        assertTrue(changeSet.changes().stream().noneMatch(change -> change instanceof CommandChange));
        ChangeSet both = service.diff(without(before(), COMMANDS), without(after(), COMMANDS));
        assertEquals(withFolders(skipped(ChangeDomain.COMMANDS, SkipSide.BOTH)), both.skipped());
        ChangeSet first = service.diff(without(before(), COMMANDS), after());
        assertEquals(withFolders(skipped(ChangeDomain.COMMANDS, SkipSide.FROM)), first.skipped());
    }

    @Test
    void severalDomainsSkippedOnDifferentSidesAreListedInTheOrderOfTheSchema() {
        ChangeSet changeSet = service.diff(without(without(before(), COMMANDS), PACKETS), without(after(), BLOCKS));
        assertEquals(
                withFolders(skipped(ChangeDomain.BLOCKS, SkipSide.TO), skipped(ChangeDomain.PACKETS, SkipSide.FROM),
                        skipped(ChangeDomain.COMMANDS, SkipSide.FROM)),
                changeSet.skipped());
        assertEquals(14, changeSet.changes().size());
    }

    @Test
    void aVersionWithoutAnyReportSkipsEveryDomainInTheOrderOfTheSchema() {
        ChangeSet changeSet = service.diff(tree("1.0", Map.of()), tree("1.1", Map.of()));
        assertEquals(
                List.of(skipped(ChangeDomain.REGISTRIES, SkipSide.BOTH), skipped(ChangeDomain.BLOCKS, SkipSide.BOTH),
                        skipped(ChangeDomain.PACKETS, SkipSide.BOTH), skipped(ChangeDomain.COMMANDS, SkipSide.BOTH),
                        skipped(ChangeDomain.RECIPES, SkipSide.BOTH), skipped(ChangeDomain.LOOT_TABLES, SkipSide.BOTH),
                        skipped(ChangeDomain.TAGS, SkipSide.BOTH)),
                changeSet.skipped());
        assertEquals(List.of(), changeSet.changes());
    }

    @Test
    void aMissingReportIsNeverReadAsEverythingAddedOrRemoved() {
        ChangeSet changeSet = service.diff(tree("1.0", Map.of()), after());
        assertEquals(
                List.of(skipped(ChangeDomain.REGISTRIES, SkipSide.FROM), skipped(ChangeDomain.BLOCKS, SkipSide.FROM),
                        skipped(ChangeDomain.PACKETS, SkipSide.FROM), skipped(ChangeDomain.COMMANDS, SkipSide.FROM),
                        skipped(ChangeDomain.RECIPES, SkipSide.FROM), skipped(ChangeDomain.LOOT_TABLES, SkipSide.FROM),
                        skipped(ChangeDomain.TAGS, SkipSide.FROM)),
                changeSet.skipped());
        assertEquals(List.of(), changeSet.changes());
    }

    @Test
    void theDataFolderMissingFromOneVersionSkipsTheThreeDataDomainsAndDeclaresNoFolder() {
        ChangeSet changeSet = service.diff(before(), onlyReports(after()));
        assertEquals(dataSkipped(SkipSide.TO), changeSet.skipped());
        assertTrue(changeSet.changes().stream().noneMatch(change -> change instanceof RecipeChange));
    }

    @Test
    void aSingularFolderAfterAPluralOneIsNotEverythingRemovedAndAdded() {
        ChangeSet changeSet = service.diff(before(), after());
        assertTrue(changeSet.changes().stream().noneMatch(change -> change instanceof RecipeRemoved removed
                && removed.recipe().equals(recipe("acorn_bread"))));
        assertTrue(changeSet.changes().stream().noneMatch(change -> change instanceof RecipeAdded added
                && added.recipe().equals(recipe("acorn_bread"))));
        assertTrue(changeSet.skipped().stream().noneMatch(entry -> entry instanceof SkippedFolder folder && folder.folder().equals("advancement")));
    }

    @Test
    void anIllegibleDataFileStopsTheDiffAndNamesThatVersionAndFile() {
        VersionTree broken = with(after(), "data/demo/recipe/gadget.json", "{\"type\": ");
        VersionReadException failure = assertThrows(VersionReadException.class, () -> service.diff(before(), broken));
        assertTrue(failure.getMessage().startsWith("Version 1.1, data/demo/recipe/gadget.json: not valid JSON"), failure.getMessage());
    }

    @Test
    void anIllegibleReportOfAFolderWithoutADomainStopsTheDiffToo() {
        VersionTree broken = with(after(), "data/demo/worldgen/biome/plains.json", "[1,");
        VersionReadException failure = assertThrows(VersionReadException.class, () -> service.diff(before(), broken));
        assertTrue(failure.getMessage().startsWith("Version 1.1, data/demo/worldgen/biome/plains.json: not valid JSON"), failure.getMessage());
    }

    @Test
    void anIllegibleRegistriesReportStopsTheDiffAndNamesThatVersion() {
        VersionTree broken = tree("1.1", Map.of(REGISTRIES, "{\"demo:gadget\": []}", BLOCKS, GadgetScenario.BLOCKS_AFTER,
                PACKETS, GadgetScenario.PACKETS_AFTER, COMMANDS, GadgetScenario.COMMANDS_AFTER));
        VersionReadException failure = assertThrows(VersionReadException.class, () -> service.diff(before(), broken));
        assertEquals("Version 1.1, reports/registries.json: registry demo:gadget is not an object", failure.getMessage());
    }

    @Test
    void anIllegibleBlocksReportStopsTheDiffAndNamesThatVersion() {
        VersionTree broken = tree("1.1", Map.of(REGISTRIES, GadgetScenario.AFTER, BLOCKS, "{\"demo:crate\": []}",
                PACKETS, GadgetScenario.PACKETS_AFTER, COMMANDS, GadgetScenario.COMMANDS_AFTER));
        VersionReadException failure = assertThrows(VersionReadException.class, () -> service.diff(before(), broken));
        assertEquals("Version 1.1, reports/blocks.json: block demo:crate is not an object", failure.getMessage());
    }

    @Test
    void anIllegiblePacketsReportStopsTheDiffAndNamesThatVersion() {
        VersionTree broken = tree("1.1", Map.of(REGISTRIES, GadgetScenario.AFTER, BLOCKS, GadgetScenario.BLOCKS_AFTER,
                PACKETS, "{\"play\": []}", COMMANDS, GadgetScenario.COMMANDS_AFTER));
        VersionReadException failure = assertThrows(VersionReadException.class, () -> service.diff(before(), broken));
        assertEquals("Version 1.1, reports/packets.json: phase play is not an object", failure.getMessage());
    }

    @Test
    void anIllegibleCommandsReportStopsTheDiffAndNamesThatVersion() {
        VersionTree broken = tree("1.1", Map.of(REGISTRIES, GadgetScenario.AFTER, BLOCKS, GadgetScenario.BLOCKS_AFTER,
                PACKETS, GadgetScenario.PACKETS_AFTER, COMMANDS, "{\"type\": \"literal\"}"));
        VersionReadException failure = assertThrows(VersionReadException.class, () -> service.diff(before(), broken));
        assertEquals("Version 1.1, reports/commands.json: the top node has type 'literal' instead of 'root'", failure.getMessage());
    }

    @Test
    void anIllegibleReportStaysAStrongErrorEvenWhenTheOtherVersionHasNone() {
        VersionTree broken = tree("1.1", Map.of(BLOCKS, "{\"demo:crate\": []}"));
        VersionReadException failure = assertThrows(VersionReadException.class, () -> service.diff(tree("1.0", Map.of()), broken));
        assertEquals("Version 1.1, reports/blocks.json: block demo:crate is not an object", failure.getMessage());
    }
}
