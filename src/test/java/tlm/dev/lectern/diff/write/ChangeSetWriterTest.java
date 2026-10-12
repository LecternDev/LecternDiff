package tlm.dev.lectern.diff.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tlm.dev.lectern.diff.fixtures.ChangeFixtures.oneOfEachType;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.entry;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.key;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.json.CanonicalJsonWriter;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.model.RegistryDefaultModified;
import tlm.dev.lectern.diff.model.VersionId;
import tlm.dev.lectern.diff.model.VersionKind;
import tlm.dev.lectern.diff.model.VersionRef;

class ChangeSetWriterTest {

    private static final String ONE_OF_EACH_TYPE = """
            {
              "changes": [
                {
                  "count": 2,
                  "first_id": 3,
                  "type": "registry.ids_shifted"
                },
                {
                  "entry_count": 4,
                  "protocol_id": 9,
                  "registry": "demo:alpha",
                  "type": "registry.added"
                },
                {
                  "entry_count": 2,
                  "protocol_id": 8,
                  "registry": "demo:beta",
                  "type": "registry.removed"
                },
                {
                  "new_id": 2,
                  "old_id": 1,
                  "registry": "demo:delta",
                  "type": "registry.id_modified"
                },
                {
                  "entry": "demo:a",
                  "protocol_id": 0,
                  "registry": "demo:epsilon",
                  "type": "registry.entry_added"
                },
                {
                  "entry": "demo:b",
                  "protocol_id": 5,
                  "registry": "demo:epsilon",
                  "type": "registry.entry_removed"
                },
                {
                  "entry": "demo:c",
                  "new_id": 2,
                  "old_id": 1,
                  "registry": "demo:epsilon",
                  "type": "registry.entry_id_modified"
                },
                {
                  "new_default": "demo:one",
                  "old_default": null,
                  "registry": "demo:gamma",
                  "type": "registry.default_modified"
                },
                {
                  "count": 7,
                  "first_id": 4,
                  "registry": "demo:zeta",
                  "type": "registry.entry_ids_shifted"
                },
                {
                  "block_count": 2,
                  "first_id": 3,
                  "state_count": 5,
                  "type": "block.state_ids_shifted"
                },
                {
                  "block": "demo:anvil",
                  "state_count": 1,
                  "type": "block.added"
                },
                {
                  "block": "demo:bell",
                  "state_count": 4,
                  "type": "block.removed"
                },
                {
                  "block": "demo:crate",
                  "property": "facing",
                  "type": "block.property_added",
                  "values": [
                    "north",
                    "south"
                  ]
                },
                {
                  "block": "demo:crate",
                  "property": "lit",
                  "type": "block.property_removed",
                  "values": [
                    "true",
                    "false"
                  ]
                },
                {
                  "block": "demo:door",
                  "property": "half",
                  "type": "block.property_values_added",
                  "values": [
                    "upper"
                  ]
                },
                {
                  "block": "demo:door",
                  "property": "hinge",
                  "type": "block.property_values_removed",
                  "values": [
                    "middle"
                  ]
                },
                {
                  "block": "demo:lamp",
                  "new_default": {
                    "dim": "low",
                    "lit": "false"
                  },
                  "old_default": {
                    "lit": "false"
                  },
                  "type": "block.default_state_modified"
                },
                {
                  "block": "demo:lever",
                  "new_first_id": 14,
                  "old_first_id": 10,
                  "state_count": 2,
                  "type": "block.state_ids_modified"
                }
              ],
              "from": {
                "id": "1.0",
                "kind": "release"
              },
              "schema_version": 1,
              "to": {
                "id": "1.1-pre-1",
                "kind": "pre-release"
              }
            }
            """;

    private final ChangeSetWriter writer = new ChangeSetWriter(new RegistryChangeRenderer(), new BlockChangeRenderer(), new CanonicalJsonWriter());

    private static ChangeSet oneOfEachTypeBetweenTwoVersions() {
        VersionRef to = new VersionRef(new VersionId("1.1-pre-1"), new VersionKind.PreRelease());
        return new ChangeSet(release("1.0"), to, oneOfEachType());
    }

    @Test
    void writesTheEnvelopeAndEveryChangeTypeInCanonicalJson() {
        assertEquals(ONE_OF_EACH_TYPE, writer.write(oneOfEachTypeBetweenTwoVersions()));
    }

    @Test
    void twoWritesGiveTheSameBytes() {
        ChangeSet changeSet = oneOfEachTypeBetweenTwoVersions();
        assertEquals(writer.write(changeSet), writer.write(changeSet));
    }

    @Test
    void theOutputIsItsOwnCanonicalForm() {
        String written = writer.write(oneOfEachTypeBetweenTwoVersions());
        assertEquals(written, new CanonicalJsonWriter().write(new JsonReader().read(written)));
    }

    @Test
    void theInputOrderOfTheChangesDoesNotChangeTheOutput() {
        List<Change> reversed = oneOfEachType().reversed();
        ChangeSet shuffled = new ChangeSet(release("1.0"),
                new VersionRef(new VersionId("1.1-pre-1"), new VersionKind.PreRelease()), reversed);
        assertEquals(ONE_OF_EACH_TYPE, writer.write(shuffled));
    }

    @Test
    void anEmptyChangeSetWritesAnEmptyList() {
        String written = writer.write(new ChangeSet(release("1.0"), release("1.1"), List.of()));
        assertEquals("""
                {
                  "changes": [],
                  "from": {
                    "id": "1.0",
                    "kind": "release"
                  },
                  "schema_version": 1,
                  "to": {
                    "id": "1.1",
                    "kind": "release"
                  }
                }
                """, written);
    }

    @Test
    void aDroppedDefaultIsWrittenAsNull() {
        ChangeSet changeSet = new ChangeSet(release("1.0"), release("1.1"),
                List.of(new RegistryDefaultModified(key("demo:gamma"), Optional.of(entry("demo:one")), Optional.empty())));
        String written = writer.write(changeSet);
        assertTrue(written.contains("\"new_default\": null,\n"), written);
        assertTrue(written.contains("\"old_default\": \"demo:one\",\n"), written);
    }

    @Test
    void outputUsesLineFeedsOnlyAndEndsWithOneNewline() {
        String written = writer.write(oneOfEachTypeBetweenTwoVersions());
        assertFalse(written.contains("\r"));
        assertTrue(written.endsWith("}\n"));
        assertFalse(written.endsWith("\n\n"));
    }
}
