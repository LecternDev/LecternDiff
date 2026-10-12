package tlm.dev.lectern.diff.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.entry;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.id;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.key;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.registries;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.registry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RegistryTest {

    @Test
    void entriesAreIteratedInKeyOrderWhateverTheInsertionOrder() {
        Registry gadget = registry("demo:gadget", 0).entry("demo:c", 2).entry("demo:a", 0).entry("demo:b", 1).build();
        assertEquals(List.of(entry("demo:a"), entry("demo:b"), entry("demo:c")), List.copyOf(gadget.entries().keySet()));
    }

    @Test
    void entriesCannotBeModifiedAfterConstruction() {
        Registry gadget = registry("demo:gadget", 0).entry("demo:a", 0).build();
        assertThrows(UnsupportedOperationException.class, () -> gadget.entries().put(entry("demo:b"), id(1)));
    }

    @Test
    void defaultEntryMustBeAnEntryOfTheRegistry() {
        assertThrows(IllegalArgumentException.class,
                () -> registry("demo:gadget", 0).defaultEntry("demo:missing").entry("demo:a", 0).build());
    }

    @Test
    void defaultEntryIsOptional() {
        assertEquals(Optional.empty(), registry("demo:gadget", 0).entry("demo:a", 0).build().defaultEntry());
        assertEquals(Optional.of(entry("demo:a")),
                registry("demo:gadget", 0).defaultEntry("demo:a").entry("demo:a", 0).build().defaultEntry());
    }

    @Test
    void twoEntriesCannotShareAProtocolId() {
        assertThrows(IllegalArgumentException.class,
                () -> registry("demo:gadget", 0).entry("demo:a", 3).entry("demo:b", 3).build());
    }

    @Test
    void twoRegistriesCannotShareAProtocolId() {
        assertThrows(IllegalArgumentException.class,
                () -> registries(registry("demo:gadget", 1).build(), registry("demo:gizmo", 1).build()));
    }

    @Test
    void registriesMustBeStoredUnderTheirOwnKey() {
        Registry gadget = registry("demo:gadget", 0).build();
        assertThrows(IllegalArgumentException.class, () -> new Registries(Map.of(key("demo:gizmo"), gadget)));
    }

    @Test
    void registryIdsAreListedByKey() {
        Registries all = registries(registry("demo:gizmo", 1).build(), registry("demo:gadget", 0).build());
        assertEquals(List.of(key("demo:gadget"), key("demo:gizmo")), List.copyOf(all.ids().keySet()));
        assertEquals(id(1), all.ids().get(key("demo:gizmo")));
    }
}
