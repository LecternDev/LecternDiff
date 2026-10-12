package tlm.dev.lectern.diff.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.entry;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.id;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.key;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.fixtures.InMemoryVersionTree;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.model.Registries;
import tlm.dev.lectern.diff.model.Registry;

class RegistryReportReaderTest {

    private final RegistryReportReader reader = new RegistryReportReader(new JsonReader());

    private static VersionTree tree(String registriesJson) {
        return new InMemoryVersionTree(release("9.1"), Map.of("reports/registries.json", registriesJson));
    }

    private String failureOf(String registriesJson) {
        return assertThrows(VersionReadException.class, () -> reader.read(tree(registriesJson))).getMessage();
    }

    private static void assertFailure(String message, String fragment) {
        assertTrue(message.startsWith("Version 9.1, reports/registries.json: "), message);
        assertTrue(message.contains(fragment), message);
    }

    @Test
    void readsRegistriesWithEntriesAndAnOptionalDefault() {
        Registries registries = reader.read(tree("""
                {
                  "demo:gadget": {
                    "default": "demo:basic",
                    "entries": {
                      "demo:basic": {"protocol_id": 0},
                      "demo:shiny": {"protocol_id": 1}
                    },
                    "protocol_id": 0
                  },
                  "demo:gizmo": {
                    "entries": {},
                    "protocol_id": 1
                  }
                }
                """));
        Registry gadget = registries.byKey().get(key("demo:gadget"));
        assertEquals(id(0), gadget.id());
        assertEquals(Optional.of(entry("demo:basic")), gadget.defaultEntry());
        assertEquals(id(1), gadget.entries().get(entry("demo:shiny")));
        Registry gizmo = registries.byKey().get(key("demo:gizmo"));
        assertEquals(Optional.empty(), gizmo.defaultEntry());
        assertEquals(Map.of(), gizmo.entries());
    }

    @Test
    void theRegistryProtocolIdMayComeBeforeOrAfterTheEntries() {
        Registries before = reader.read(tree("""
                {"demo:gadget": {"protocol_id": 4, "entries": {"demo:basic": {"protocol_id": 0}}}}
                """));
        Registries after = reader.read(tree("""
                {"demo:gadget": {"entries": {"demo:basic": {"protocol_id": 0}}, "protocol_id": 4}}
                """));
        assertEquals(before, after);
    }

    @Test
    void theDefaultMayComeFirstAndRegistriesMayBeUnsorted() {
        Registries registries = reader.read(tree("""
                {
                  "demo:zeta": {"default": "demo:a", "protocol_id": 1, "entries": {"demo:a": {"protocol_id": 0}}},
                  "demo:alpha": {"entries": {}, "protocol_id": 0}
                }
                """));
        assertEquals(List.of(key("demo:alpha"), key("demo:zeta")), List.copyOf(registries.byKey().keySet()));
    }

    @Test
    void aMissingReportNamesTheVersionAndTheFile() {
        VersionTree empty = new InMemoryVersionTree(release("9.1"), Map.of());
        VersionReadException failure = assertThrows(VersionReadException.class, () -> reader.read(empty));
        assertEquals("Version 9.1, reports/registries.json: the report is missing", failure.getMessage());
    }

    @Test
    void invalidJsonIsRefusedWithTheVersionAndTheFile() {
        String message = failureOf("{\"demo:gadget\": ");
        assertFailure(message, "not valid JSON");
    }

    @Test
    void aDocumentThatIsNotAnObjectIsRefused() {
        String message = failureOf("[]");
        assertFailure(message, "the document is not an object");
    }

    @Test
    void aRegistryThatIsNotAnObjectIsRefused() {
        String message = failureOf("{\"demo:gadget\": 3}");
        assertFailure(message, "registry demo:gadget is not an object");
    }

    @Test
    void aRegistryWithoutEntriesIsRefused() {
        String message = failureOf("{\"demo:gadget\": {\"protocol_id\": 0}}");
        assertFailure(message, "missing key 'entries' in registry demo:gadget");
    }

    @Test
    void aRegistryWithoutProtocolIdIsRefused() {
        String message = failureOf("{\"demo:gadget\": {\"entries\": {}}}");
        assertFailure(message, "missing key 'protocol_id' in registry demo:gadget");
    }

    @Test
    void anUnknownKeyInARegistryIsRefused() {
        String message = failureOf("{\"demo:gadget\": {\"entries\": {}, \"protocol_id\": 0, \"tags\": []}}");
        assertFailure(message, "unknown key 'tags' in registry demo:gadget");
    }

    @Test
    void anUnknownKeyInAnEntryIsRefused() {
        String message = failureOf("""
                {"demo:gadget": {"entries": {"demo:basic": {"protocol_id": 0, "weight": 3}}, "protocol_id": 0}}
                """);
        assertFailure(message, "unknown key 'weight' in entry demo:basic of registry demo:gadget");
    }

    @Test
    void anEntryWithoutProtocolIdIsRefused() {
        String message = failureOf("{\"demo:gadget\": {\"entries\": {\"demo:basic\": {}}, \"protocol_id\": 0}}");
        assertFailure(message, "missing key 'protocol_id' in entry demo:basic of registry demo:gadget");
    }

    @Test
    void aProtocolIdThatIsNotAWholeNonNegativeNumberIsRefused() {
        for (String bad : new String[] {"\"3\"", "1.0", "1e2", "-1", "99999999999", "true", "null"}) {
            String message = failureOf(
                    "{\"demo:gadget\": {\"entries\": {\"demo:basic\": {\"protocol_id\": " + bad + "}}, \"protocol_id\": 0}}");
            assertFailure(message, "protocol_id of entry demo:basic of registry demo:gadget is not");
        }
    }

    @Test
    void aDefaultThatIsNotAnEntryIsRefused() {
        String message = failureOf("""
                {"demo:gadget": {"default": "demo:ghost", "entries": {"demo:basic": {"protocol_id": 0}}, "protocol_id": 0}}
                """);
        assertFailure(message, "Default entry demo:ghost is not an entry of registry demo:gadget");
    }

    @Test
    void aDefaultThatIsNotAStringIsRefused() {
        String message = failureOf("{\"demo:gadget\": {\"default\": 3, \"entries\": {}, \"protocol_id\": 0}}");
        assertFailure(message, "default of registry demo:gadget is not a string");
    }

    @Test
    void twoEntriesWithTheSameProtocolIdAreRefused() {
        String message = failureOf("""
                {"demo:gadget": {"entries": {"demo:a": {"protocol_id": 2}, "demo:b": {"protocol_id": 2}}, "protocol_id": 0}}
                """);
        assertFailure(message, "Protocol id 2 is used twice in registry demo:gadget");
    }

    @Test
    void twoRegistriesWithTheSameProtocolIdAreRefused() {
        String message = failureOf("""
                {"demo:a": {"entries": {}, "protocol_id": 0}, "demo:b": {"entries": {}, "protocol_id": 0}}
                """);
        assertFailure(message, "used by two registries");
    }

    @Test
    void aKeyThatIsNotNamespacedIsRefused() {
        String message = failureOf("{\"gadget\": {\"entries\": {}, \"protocol_id\": 0}}");
        assertFailure(message, "Invalid registry key: 'gadget'");
    }
}
