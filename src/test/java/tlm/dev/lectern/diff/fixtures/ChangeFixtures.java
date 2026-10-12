package tlm.dev.lectern.diff.fixtures;

import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.entry;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.id;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.key;

import java.util.List;
import java.util.Optional;
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
                new RegistryEntryIdsShifted(key("demo:zeta"), 7, id(4)));
    }
}
