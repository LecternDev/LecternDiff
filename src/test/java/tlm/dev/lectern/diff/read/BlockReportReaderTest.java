package tlm.dev.lectern.diff.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.blockKey;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.property;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.state;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.stateId;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.values;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.fixtures.InMemoryVersionTree;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.model.Block;
import tlm.dev.lectern.diff.model.Blocks;

class BlockReportReaderTest {

    private final BlockReportReader reader = new BlockReportReader(new JsonReader());

    private Blocks read(VersionTree tree) {
        return reader.read(tree).orElseThrow();
    }

    private static VersionTree tree(String blocksJson) {
        return new InMemoryVersionTree(release("9.1"), Map.of("reports/blocks.json", blocksJson));
    }

    private String failureOf(String blocksJson) {
        return assertThrows(VersionReadException.class, () -> read(tree(blocksJson))).getMessage();
    }

    private static void assertFailure(String message, String fragment) {
        assertTrue(message.startsWith("Version 9.1, reports/blocks.json: "), message);
        assertTrue(message.contains(fragment), message);
    }

    @Test
    void readsBlocksWithAndWithoutProperties() {
        Blocks blocks = read(tree("""
                {
                  "demo:crate": {"states": [{"default": true, "id": 0}]},
                  "demo:lever": {
                    "properties": {"face": ["floor", "wall"], "powered": ["true", "false"]},
                    "states": [
                      {"id": 1, "properties": {"face": "floor", "powered": "true"}},
                      {"id": 2, "properties": {"face": "floor", "powered": "false"}},
                      {"default": true, "id": 3, "properties": {"face": "wall", "powered": "true"}},
                      {"id": 4, "properties": {"face": "wall", "powered": "false"}}
                    ]
                  }
                }
                """));
        Block crate = blocks.byKey().get(blockKey("demo:crate"));
        assertEquals(Map.of(), crate.properties());
        assertEquals(Map.of(state(""), stateId(0)), crate.states());
        Block lever = blocks.byKey().get(blockKey("demo:lever"));
        assertEquals(values("floor", "wall"), lever.properties().get(property("face")));
        assertEquals(values("true", "false"), lever.properties().get(property("powered")));
        assertEquals(state("face=wall,powered=true"), lever.defaultState());
        assertEquals(stateId(4), lever.states().get(state("face=wall,powered=false")));
        assertEquals(5, blocks.stateCount());
    }

    @Test
    void theOrderOfTheKeysDoesNotMatterInBlocksOrInStates() {
        Blocks first = read(tree("""
                {"demo:lamp": {
                  "properties": {"lit": ["true", "false"]},
                  "states": [
                    {"properties": {"lit": "true"}, "id": 1},
                    {"default": true, "properties": {"lit": "false"}, "id": 2}
                  ]}}
                """));
        Blocks second = read(tree("""
                {"demo:lamp": {
                  "states": [
                    {"id": 2, "properties": {"lit": "false"}, "default": true},
                    {"id": 1, "properties": {"lit": "true"}}
                  ],
                  "properties": {"lit": ["true", "false"]}}}
                """));
        assertEquals(first, second);
    }

    @Test
    void theDefinitionOfABlockIsIgnoredWhateverItHolds() {
        Blocks withDefinition = read(tree("""
                {
                  "demo:a": {"definition": {"type": "demo:plain", "settings": [1, 2, {"x": null}]}, "states": [{"default": true, "id": 0}]},
                  "demo:b": {"definition": "free text", "states": [{"default": true, "id": 1}]},
                  "demo:c": {"definition": 3, "states": [{"default": true, "id": 2}]}
                }
                """));
        Blocks without = read(tree("""
                {
                  "demo:a": {"states": [{"default": true, "id": 0}]},
                  "demo:b": {"states": [{"default": true, "id": 1}]},
                  "demo:c": {"states": [{"default": true, "id": 2}]}
                }
                """));
        assertEquals(without, withDefinition);
    }

    @Test
    void blocksMayBeUnsortedAndStateIdsMayHaveHoles() {
        Blocks blocks = read(tree("""
                {
                  "demo:zeta": {"states": [{"default": true, "id": 40}]},
                  "demo:alpha": {"states": [{"default": true, "id": 7}]}
                }
                """));
        assertEquals(List.of(blockKey("demo:alpha"), blockKey("demo:zeta")), List.copyOf(blocks.byKey().keySet()));
    }

    @Test
    void aMissingReportGivesNoBlocksInsteadOfAnError() {
        VersionTree empty = new InMemoryVersionTree(release("9.1"), Map.of());
        assertEquals(Optional.empty(), reader.read(empty));
    }

    @Test
    void invalidJsonAndShapesOfTheDocumentAreRefused() {
        assertFailure(failureOf("{\"demo:a\": "), "not valid JSON");
        assertFailure(failureOf("[]"), "the document is not an object");
        assertFailure(failureOf("{\"demo:a\": 3}"), "block demo:a is not an object");
    }

    @Test
    void anUnknownKeyInABlockIsRefused() {
        assertFailure(failureOf("{\"demo:a\": {\"hardness\": 3, \"states\": [{\"default\": true, \"id\": 0}]}}"),
                "unknown key 'hardness' in block demo:a");
    }

    @Test
    void anUnknownKeyInAStateIsRefused() {
        assertFailure(failureOf("{\"demo:a\": {\"states\": [{\"default\": true, \"id\": 0, \"weight\": 3}]}}"),
                "unknown key 'weight' in a state of block demo:a");
    }

    @Test
    void aBlockWithoutStatesIsRefused() {
        assertFailure(failureOf("{\"demo:a\": {}}"), "missing key 'states' in block demo:a");
        assertFailure(failureOf("{\"demo:a\": {\"states\": {}}}"), "states of block demo:a is not an array");
        assertFailure(failureOf("{\"demo:a\": {\"states\": []}}"), "block demo:a has no default state");
    }

    @Test
    void aStateWithoutAnIdOrWithAnIdThatIsNotAWholeNonNegativeNumberIsRefused() {
        assertFailure(failureOf("{\"demo:a\": {\"states\": [{\"default\": true}]}}"),
                "missing key 'id' in a state of block demo:a");
        for (String bad : List.of("\"3\"", "1.0", "1e2", "-1", "99999999999", "true", "null")) {
            assertFailure(failureOf("{\"demo:a\": {\"states\": [{\"default\": true, \"id\": " + bad + "}]}}"),
                    "id of a state of block demo:a is not");
        }
    }

    @Test
    void twoStatesWithTheSameIdAreRefusedInsideABlockAndAcrossBlocks() {
        assertFailure(failureOf("""
                {"demo:a": {"properties": {"lit": ["true", "false"]}, "states": [
                  {"default": true, "id": 1, "properties": {"lit": "true"}},
                  {"id": 1, "properties": {"lit": "false"}}]}}
                """), "State id 1 is used twice in block demo:a");
        assertFailure(failureOf("""
                {"demo:a": {"states": [{"default": true, "id": 1}]},
                 "demo:b": {"states": [{"default": true, "id": 1}]}}
                """), "State id 1 is used by more than one state");
    }

    @Test
    void aBlockWithNoDefaultStateOrSeveralIsRefused() {
        String twoStates = "\"properties\": {\"lit\": [\"true\", \"false\"]}, \"states\": [";
        assertFailure(failureOf("{\"demo:a\": {" + twoStates
                + "{\"id\": 1, \"properties\": {\"lit\": \"true\"}}, {\"id\": 2, \"properties\": {\"lit\": \"false\"}}]}}"),
                "block demo:a has no default state");
        assertFailure(failureOf("{\"demo:a\": {" + twoStates
                + "{\"default\": true, \"id\": 1, \"properties\": {\"lit\": \"true\"}}, "
                + "{\"default\": true, \"id\": 2, \"properties\": {\"lit\": \"false\"}}]}}"),
                "block demo:a has 2 default states");
    }

    @Test
    void aDefaultFlagThatIsNotTrueIsRefused() {
        for (String bad : List.of("false", "\"yes\"", "1", "null")) {
            assertFailure(failureOf("{\"demo:a\": {\"states\": [{\"default\": " + bad + ", \"id\": 0}]}}"),
                    "default of a state of block demo:a is not true");
        }
    }

    @Test
    void aStateListedTwiceWithTheSamePropertiesIsRefused() {
        assertFailure(failureOf("""
                {"demo:a": {"properties": {"lit": ["true"]}, "states": [
                  {"default": true, "id": 1, "properties": {"lit": "true"}},
                  {"id": 2, "properties": {"lit": "true"}}]}}
                """), "block demo:a lists the state {lit=true} twice");
    }

    @Test
    void aStateThatDoesNotMatchTheDeclaredPropertiesIsRefused() {
        assertFailure(failureOf("""
                {"demo:a": {"properties": {"lit": ["true", "false"]}, "states": [
                  {"default": true, "id": 1, "properties": {"lit": "maybe"}}]}}
                """), "sets lit to maybe, which the block does not declare");
        assertFailure(failureOf("""
                {"demo:a": {"properties": {"lit": ["true"]}, "states": [{"default": true, "id": 1}]}}
                """), "sets [] but the block declares [lit]");
    }

    @Test
    void propertiesThatAreMalformedAreRefused() {
        assertFailure(failureOf("{\"demo:a\": {\"properties\": [], \"states\": [{\"default\": true, \"id\": 0}]}}"),
                "properties of block demo:a is not an object");
        assertFailure(failureOf("{\"demo:a\": {\"properties\": {\"lit\": \"true\"}, \"states\": [{\"default\": true, \"id\": 0}]}}"),
                "values of property lit of block demo:a is not an array");
        assertFailure(failureOf("{\"demo:a\": {\"properties\": {\"lit\": [true]}, \"states\": [{\"default\": true, \"id\": 0}]}}"),
                "a value of property lit of block demo:a is not a string");
        assertFailure(failureOf("{\"demo:a\": {\"properties\": {\"lit\": []}, \"states\": [{\"default\": true, \"id\": 0}]}}"),
                "Property lit of block demo:a has no value");
        assertFailure(failureOf("{\"demo:a\": {\"properties\": {\"lit\": [\"x\", \"x\"]}, \"states\": [{\"default\": true, \"id\": 0}]}}"),
                "Property lit of block demo:a lists a value twice");
    }

    @Test
    void namesThatBreakTheSyntaxAreRefused() {
        assertFailure(failureOf("{\"crate\": {\"states\": [{\"default\": true, \"id\": 0}]}}"), "Invalid block key: 'crate'");
        assertFailure(failureOf("{\"demo:a\": {\"properties\": {\"Lit\": [\"true\"]}, \"states\": [{\"default\": true, \"id\": 0}]}}"),
                "Invalid property name: 'Lit'");
        assertFailure(failureOf("{\"demo:a\": {\"properties\": {\"lit\": [\"True\"]}, \"states\": [{\"default\": true, \"id\": 0}]}}"),
                "Invalid property value: 'True'");
    }

    @Test
    void aStatePropertyThatIsNotAStringIsRefused() {
        assertFailure(failureOf("""
                {"demo:a": {"properties": {"lit": ["true"]}, "states": [
                  {"default": true, "id": 1, "properties": {"lit": true}}]}}
                """), "the value of property lit of a state of block demo:a is not a string");
    }
}
