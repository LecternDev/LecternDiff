package tlm.dev.lectern.diff.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tlm.dev.lectern.diff.fixtures.ChangeFixtures.oneOfEachType;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.json.CanonicalJsonWriter;
import tlm.dev.lectern.diff.json.JsonArray;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.json.JsonString;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeSet;

class ChangeTypeNamesTest {

    private static final Set<String> STABLE_NAMES = Set.of(
            "registry.added",
            "registry.removed",
            "registry.default_modified",
            "registry.id_modified",
            "registry.ids_shifted",
            "registry.entry_added",
            "registry.entry_removed",
            "registry.entry_id_modified",
            "registry.entry_ids_shifted",
            "block.added",
            "block.removed",
            "block.property_added",
            "block.property_removed",
            "block.property_values_added",
            "block.property_values_removed",
            "block.default_state_modified",
            "block.state_ids_shifted",
            "block.state_ids_modified");

    private static List<Class<?>> leaves(Class<?> type) {
        if (!type.isSealed()) {
            return List.of(type);
        }
        return Arrays.stream(type.getPermittedSubclasses()).flatMap(subtype -> leaves(subtype).stream()).toList();
    }

    @Test
    void theFixtureHoldsExactlyOneChangeOfEveryPermittedType() {
        Set<Class<?>> fixtureTypes = oneOfEachType().stream().<Class<?>>map(Object::getClass).collect(Collectors.toSet());
        assertEquals(Set.copyOf(leaves(Change.class)), fixtureTypes);
        assertEquals(leaves(Change.class).size(), oneOfEachType().size());
    }

    @Test
    void everyTypeIsWrittenUnderItsOwnStableName() {
        ChangeSetWriter writer = new ChangeSetWriter(new RegistryChangeRenderer(), new BlockChangeRenderer(), new CanonicalJsonWriter());
        String written = writer.write(new ChangeSet(release("1.0"), release("1.1"), oneOfEachType()));
        JsonObject document = (JsonObject) new JsonReader().read(written);
        JsonArray changes = (JsonArray) document.member("changes").orElseThrow();
        List<String> names = changes.elements().stream()
                .map(change -> ((JsonString) ((JsonObject) change).member("type").orElseThrow()).value())
                .toList();
        assertEquals(oneOfEachType().size(), names.size());
        assertEquals(STABLE_NAMES, Set.copyOf(names));
    }
}
