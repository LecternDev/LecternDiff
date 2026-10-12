package tlm.dev.lectern.diff.fixtures;

import static tlm.dev.lectern.diff.fixtures.BlockFixtures.blockKey;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.property;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.state;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.stateId;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.values;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.entry;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.id;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.key;

import java.util.List;
import java.util.Optional;
import tlm.dev.lectern.diff.model.BlockAdded;
import tlm.dev.lectern.diff.model.BlockDefaultStateModified;
import tlm.dev.lectern.diff.model.BlockPropertyAdded;
import tlm.dev.lectern.diff.model.BlockPropertyRemoved;
import tlm.dev.lectern.diff.model.BlockPropertyValuesAdded;
import tlm.dev.lectern.diff.model.BlockPropertyValuesRemoved;
import tlm.dev.lectern.diff.model.BlockRemoved;
import tlm.dev.lectern.diff.model.BlockStateIdsModified;
import tlm.dev.lectern.diff.model.BlockStateIdsShifted;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.RegistryAdded;
import tlm.dev.lectern.diff.model.RegistryDefaultModified;
import tlm.dev.lectern.diff.model.RegistryEntryAdded;
import tlm.dev.lectern.diff.model.RegistryEntryIdModified;
import tlm.dev.lectern.diff.model.RegistryEntryIdsShifted;
import tlm.dev.lectern.diff.model.RegistryEntryRemoved;
import tlm.dev.lectern.diff.model.RegistryIdModified;
import tlm.dev.lectern.diff.model.RegistryIdsShifted;
import tlm.dev.lectern.diff.model.RegistryRemoved;

public final class ChangeFixtures {

    private ChangeFixtures() {
    }

    public static List<Change> oneOfEachType() {
        return List.of(
                new RegistryIdsShifted(2, id(3)),
                new RegistryAdded(key("demo:alpha"), id(9), 4),
                new RegistryRemoved(key("demo:beta"), id(8), 2),
                new RegistryDefaultModified(key("demo:gamma"), Optional.empty(), Optional.of(entry("demo:one"))),
                new RegistryIdModified(key("demo:delta"), id(1), id(2)),
                new RegistryEntryAdded(key("demo:epsilon"), entry("demo:a"), id(0)),
                new RegistryEntryRemoved(key("demo:epsilon"), entry("demo:b"), id(5)),
                new RegistryEntryIdModified(key("demo:epsilon"), entry("demo:c"), id(1), id(2)),
                new RegistryEntryIdsShifted(key("demo:zeta"), 7, id(4)),
                new BlockStateIdsShifted(5, 2, stateId(3)),
                new BlockAdded(blockKey("demo:anvil"), 1),
                new BlockRemoved(blockKey("demo:bell"), 4),
                new BlockPropertyAdded(blockKey("demo:crate"), property("facing"), values("north", "south")),
                new BlockPropertyRemoved(blockKey("demo:crate"), property("lit"), values("true", "false")),
                new BlockPropertyValuesAdded(blockKey("demo:door"), property("half"), values("upper")),
                new BlockPropertyValuesRemoved(blockKey("demo:door"), property("hinge"), values("middle")),
                new BlockDefaultStateModified(blockKey("demo:lamp"), state("lit=false"), state("dim=low,lit=false")),
                new BlockStateIdsModified(blockKey("demo:lever"), 2, stateId(10), stateId(14)));
    }
}
