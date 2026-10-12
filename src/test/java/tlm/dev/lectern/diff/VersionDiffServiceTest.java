package tlm.dev.lectern.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import tlm.dev.lectern.diff.model.RegistryAdded;
import tlm.dev.lectern.diff.model.RegistryEntryAdded;
import tlm.dev.lectern.diff.model.RegistryEntryIdsShifted;
import tlm.dev.lectern.diff.model.RegistryRemoved;
import tlm.dev.lectern.diff.read.RegistryReportReader;
import tlm.dev.lectern.diff.read.VersionReadException;
import tlm.dev.lectern.diff.read.VersionTree;
import tlm.dev.lectern.diff.registries.IdShiftClassifier;
import tlm.dev.lectern.diff.registries.RegistryDiffer;

class VersionDiffServiceTest {

    private final VersionDiffService service = new VersionDiffService(
            new RegistryReportReader(new JsonReader()), new RegistryDiffer(new IdShiftClassifier()));

    private static VersionTree tree(String versionId, String registriesJson) {
        return new InMemoryVersionTree(release(versionId), Map.of("reports/registries.json", registriesJson));
    }

    @Test
    void diffsTwoVersionTreesFromTheirReports() {
        ChangeSet changeSet = service.diff(tree("1.0", GadgetScenario.BEFORE), tree("1.1", GadgetScenario.AFTER));
        assertEquals(release("1.0"), changeSet.from());
        assertEquals(release("1.1"), changeSet.to());
        assertEquals(
                List.of(new RegistryEntryAdded(key("demo:gadget"), entry("demo:glossy"), id(1)),
                        new RegistryEntryIdsShifted(key("demo:gadget"), 2, id(1)),
                        new RegistryRemoved(key("demo:gizmo"), id(1), 1),
                        new RegistryAdded(key("demo:widget"), id(1), 1)),
                changeSet.changes());
    }

    @Test
    void comparingAVersionWithItselfGivesNoChange() {
        assertEquals(List.of(), service.diff(tree("1.0", GadgetScenario.BEFORE), tree("1.0", GadgetScenario.BEFORE)).changes());
    }

    @Test
    void aVersionWhoseReportCannotBeReadStopsTheDiffAndNamesThatVersion() {
        VersionReadException failure = assertThrows(VersionReadException.class,
                () -> service.diff(tree("1.0", GadgetScenario.BEFORE), tree("1.1", "{\"demo:gadget\": []}")));
        assertEquals("Version 1.1, reports/registries.json: registry demo:gadget is not an object", failure.getMessage());
    }
}
