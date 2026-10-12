package tlm.dev.lectern.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.blockKey;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.property;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.stateId;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.value;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.values;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.entry;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.id;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.key;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.fixtures.GadgetScenario;
import tlm.dev.lectern.diff.fixtures.InMemoryVersionTree;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.blocks.BlockDiffer;
import tlm.dev.lectern.diff.model.BlockAdded;
import tlm.dev.lectern.diff.model.BlockPropertyAdded;
import tlm.dev.lectern.diff.model.BlockStateIdsShifted;
import tlm.dev.lectern.diff.model.RegistryAdded;
import tlm.dev.lectern.diff.model.RegistryChange;
import tlm.dev.lectern.diff.model.RegistryEntryAdded;
import tlm.dev.lectern.diff.model.RegistryEntryIdsShifted;
import tlm.dev.lectern.diff.model.RegistryRemoved;
import tlm.dev.lectern.diff.read.BlockReportReader;
import tlm.dev.lectern.diff.read.RegistryReportReader;
import tlm.dev.lectern.diff.read.VersionReadException;
import tlm.dev.lectern.diff.read.VersionTree;
import tlm.dev.lectern.diff.ids.IdShiftClassifier;
import tlm.dev.lectern.diff.registries.RegistryDiffer;

class VersionDiffServiceTest {

    private final VersionDiffService service = new VersionDiffService(
            new RegistryReportReader(new JsonReader()),
            new RegistryDiffer(new IdShiftClassifier()),
            new BlockReportReader(new JsonReader()),
            new BlockDiffer(new IdShiftClassifier()));

    private static VersionTree tree(String versionId, String registriesJson, String blocksJson) {
        return new InMemoryVersionTree(release(versionId),
                Map.of("reports/registries.json", registriesJson, "reports/blocks.json", blocksJson));
    }

    @Test
    void diffsTwoVersionTreesFromTheirReportsInOneSortedChangeSet() {
        ChangeSet changeSet = service.diff(
                tree("1.0", GadgetScenario.BEFORE, GadgetScenario.BLOCKS_BEFORE),
                tree("1.1", GadgetScenario.AFTER, GadgetScenario.BLOCKS_AFTER));
        assertEquals(release("1.0"), changeSet.from());
        assertEquals(release("1.1"), changeSet.to());
        assertEquals(
                List.of(new RegistryEntryAdded(key("demo:gadget"), entry("demo:glossy"), id(1)),
                        new RegistryEntryIdsShifted(key("demo:gadget"), 2, id(1)),
                        new RegistryRemoved(key("demo:gizmo"), id(1), 1),
                        new RegistryAdded(key("demo:widget"), id(1), 1),
                        new BlockStateIdsShifted(3, 2, stateId(0)),
                        new BlockAdded(blockKey("demo:anvil"), 1),
                        new BlockPropertyAdded(blockKey("demo:lamp"), property("dim"), values("low", "high"), value("low"))),
                changeSet.changes());
    }

    @Test
    void comparingAVersionWithItselfGivesNoChange() {
        VersionTree same = tree("1.0", GadgetScenario.BEFORE, GadgetScenario.BLOCKS_BEFORE);
        assertEquals(List.of(), service.diff(same, same).changes());
    }

    @Test
    void aVersionWhoseRegistriesReportCannotBeReadStopsTheDiffAndNamesThatVersion() {
        VersionReadException failure = assertThrows(VersionReadException.class, () -> service.diff(
                tree("1.0", GadgetScenario.BEFORE, GadgetScenario.BLOCKS_BEFORE),
                tree("1.1", "{\"demo:gadget\": []}", GadgetScenario.BLOCKS_AFTER)));
        assertEquals("Version 1.1, reports/registries.json: registry demo:gadget is not an object", failure.getMessage());
    }

    @Test
    void aVersionWhoseBlocksReportCannotBeReadStopsTheDiffAndNamesThatVersion() {
        VersionReadException failure = assertThrows(VersionReadException.class, () -> service.diff(
                tree("1.0", GadgetScenario.BEFORE, GadgetScenario.BLOCKS_BEFORE),
                tree("1.1", GadgetScenario.AFTER, "{\"demo:crate\": []}")));
        assertEquals("Version 1.1, reports/blocks.json: block demo:crate is not an object", failure.getMessage());
    }

    @Test
    void theChangesOfBothDomainsAreInTheOrderOfTheSchema() {
        ChangeSet changeSet = service.diff(
                tree("1.0", GadgetScenario.BEFORE, GadgetScenario.BLOCKS_BEFORE),
                tree("1.1", GadgetScenario.AFTER, GadgetScenario.BLOCKS_AFTER));
        List<Boolean> registryFlags = changeSet.changes().stream().map(change -> change instanceof RegistryChange).toList();
        assertEquals(List.of(true, true, true, true, false, false, false), registryFlags);
    }
}
