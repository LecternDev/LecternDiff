package tlm.dev.lectern.diff.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.blockKey;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.property;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.state;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.stateId;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.values;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.entry;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.id;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.key;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ChangeSetTest {

    private static final List<Change> IN_CONTRACT_ORDER = List.of(
            new RegistryIdsShifted(2, id(3)),
            new RegistryAdded(key("demo:alpha"), id(9), 4),
            new RegistryRemoved(key("demo:alpha"), id(8), 2),
            new RegistryDefaultModified(key("demo:alpha"), Optional.empty(), Optional.of(entry("demo:one"))),
            new RegistryIdModified(key("demo:alpha"), id(1), id(2)),
            new RegistryEntryAdded(key("demo:alpha"), entry("demo:a"), id(0)),
            new RegistryEntryAdded(key("demo:alpha"), entry("demo:b"), id(1)),
            new RegistryEntryRemoved(key("demo:alpha"), entry("demo:a"), id(5)),
            new RegistryEntryIdModified(key("demo:alpha"), entry("demo:a"), id(1), id(2)),
            new RegistryEntryIdsShifted(key("demo:alpha"), 7, id(4)),
            new RegistryAdded(key("demo:beta"), id(10), 0),
            new RegistryEntryAdded(key("demo:beta"), entry("demo:z"), id(0)),
            new BlockStateIdsShifted(4, 2, stateId(3)),
            new BlockAdded(blockKey("demo:anvil"), 1),
            new BlockRemoved(blockKey("demo:anvil"), 2),
            new BlockPropertyAdded(blockKey("demo:anvil"), property("a"), values("x")),
            new BlockPropertyAdded(blockKey("demo:anvil"), property("b"), values("x")),
            new BlockPropertyRemoved(blockKey("demo:anvil"), property("a"), values("x")),
            new BlockPropertyValuesAdded(blockKey("demo:anvil"), property("a"), values("y")),
            new BlockPropertyValuesRemoved(blockKey("demo:anvil"), property("a"), values("y")),
            new BlockDefaultStateModified(blockKey("demo:anvil"), state("a=x"), state("a=y")),
            new BlockStateIdsModified(blockKey("demo:anvil"), 2, stateId(1), stateId(2)),
            new BlockAdded(blockKey("demo:bell"), 3));

    @Test
    void changesAreSortedByDomainThenSubjectThenTypeThenDetail() {
        List<Change> shuffled = new ArrayList<>(IN_CONTRACT_ORDER);
        Collections.shuffle(shuffled, new Random(7));
        assertEquals(IN_CONTRACT_ORDER, new ChangeSet(release("1.0"), release("1.1"), shuffled).changes());
    }

    @Test
    void sortingGivesTheSameListForEveryInputOrder() {
        for (long seed = 0; seed < 20; seed++) {
            List<Change> shuffled = new ArrayList<>(IN_CONTRACT_ORDER);
            Collections.shuffle(shuffled, new Random(seed));
            assertEquals(IN_CONTRACT_ORDER, new ChangeSet(release("1.0"), release("1.1"), shuffled).changes(), "seed " + seed);
        }
    }

    @Test
    void changeListCannotBeModified() {
        ChangeSet changeSet = new ChangeSet(release("1.0"), release("1.1"), IN_CONTRACT_ORDER);
        assertThrows(UnsupportedOperationException.class, () -> changeSet.changes().clear());
    }

    @Test
    void schemaVersionIsOne() {
        assertEquals(1, new ChangeSet(release("1.0"), release("1.1"), List.of()).schemaVersion());
    }
}
