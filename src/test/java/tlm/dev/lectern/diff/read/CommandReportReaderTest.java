package tlm.dev.lectern.diff.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.argument;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.command;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.literal;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.tree;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.fixtures.InMemoryVersionTree;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.model.CommandTree;

class CommandReportReaderTest {

    private static final String ROOT_WITH = "{\"type\": \"root\", \"children\": ";

    private final CommandReportReader reader = new CommandReportReader(new JsonReader());

    private static VersionTree versionWith(String commandsJson) {
        return new InMemoryVersionTree(release("9.1"), Map.of("reports/commands.json", commandsJson));
    }

    private CommandTree read(String commandsJson) {
        return reader.read(versionWith(commandsJson)).orElseThrow();
    }

    private String failureOf(String commandsJson) {
        return assertThrows(VersionReadException.class, () -> reader.read(versionWith(commandsJson))).getMessage();
    }

    private static void assertFailure(String message, String fragment) {
        assertTrue(message.startsWith("Version 9.1, reports/commands.json: "), message);
        assertTrue(message.contains(fragment), message);
    }

    private static String withChildren(String children) {
        return ROOT_WITH + children + "}";
    }

    @Test
    void readsLiteralsArgumentsAndEveryNodeKey() {
        CommandTree read = read("""
                {
                  "type": "root",
                  "children": {
                    "execute": {
                      "type": "literal",
                      "children": {
                        "as": {
                          "type": "literal",
                          "children": {
                            "targets": {
                              "type": "argument",
                              "parser": "minecraft:entity",
                              "properties": {"amount": "multiple", "type": "entities"},
                              "children": {"run": {"type": "literal", "redirect": ["execute"]}}
                            }
                          }
                        }
                      }
                    },
                    "op": {
                      "type": "literal",
                      "executable": true,
                      "permissions": {"type": "minecraft:require", "permission": {"type": "minecraft:command_level", "level": "admins"}}
                    }
                  }
                }
                """);
        CommandTree expected = tree(
                command("execute", literal().child("as", literal().child("targets", argument("minecraft:entity")
                        .text("amount", "multiple").text("type", "entities").child("run", literal().redirect("execute"))))),
                command("op", literal().executable().requires("admins")));
        assertEquals(expected, read);
        assertEquals(5, read.nodeCount());
    }

    @Test
    void theOrderOfTheKeysAndOfTheChildrenDoesNotMatter() {
        CommandTree first = read(withChildren("""
                {"a": {"type": "literal", "executable": true, "children": {"x": {"type": "literal"}, "y": {"type": "literal"}}},
                 "b": {"type": "argument", "parser": "brigadier:bool", "executable": true}}
                """));
        CommandTree second = read("""
                {"children": {
                   "b": {"executable": true, "parser": "brigadier:bool", "type": "argument"},
                   "a": {"children": {"y": {"type": "literal"}, "x": {"type": "literal"}}, "executable": true, "type": "literal"}},
                 "type": "root"}
                """);
        assertEquals(first, second);
    }

    @Test
    void propertyNumbersKeepTheirWrittenText() {
        CommandTree read = read(withChildren("""
                {"a": {"type": "argument", "parser": "brigadier:double", "properties": {"min": -5.9999968E7, "max": 1.0E-5}},
                 "b": {"type": "argument", "parser": "brigadier:float", "properties": {"min": 0, "max": 0.0}}}
                """));
        assertEquals(
                tree(command("a", argument("brigadier:double").number("min", "-5.9999968E7").number("max", "1.0E-5")),
                        command("b", argument("brigadier:float").number("min", "0").number("max", "0.0"))),
                read);
    }

    @Test
    void aRootWithoutChildrenAndExecutableFalseAreAllowed() {
        assertEquals(tree(), read("{\"type\": \"root\"}"));
        assertEquals(tree(command("a", literal())), read(withChildren("{\"a\": {\"type\": \"literal\", \"executable\": false}}")));
    }

    @Test
    void aMissingReportGivesNoCommandsInsteadOfAnError() {
        assertEquals(Optional.empty(), reader.read(new InMemoryVersionTree(release("9.1"), Map.of())));
    }

    @Test
    void invalidJsonAndShapesOfTheDocumentAreRefused() {
        assertFailure(failureOf("{\"type\": "), "not valid JSON");
        assertFailure(failureOf("[]"), "the document is not an object");
        assertFailure(failureOf("{}"), "missing key 'type' in the root node");
        assertFailure(failureOf("{\"type\": \"literal\"}"), "the top node has type 'literal' instead of 'root'");
        assertFailure(failureOf("{\"type\": \"root\", \"parser\": \"x\"}"), "unknown key 'parser' in the root node");
        assertFailure(failureOf(withChildren("[]")), "children of the root node is not an object");
        assertFailure(failureOf(withChildren("{\"a\": 3}")), "node a is not an object");
    }

    @Test
    void aNodeWithoutATypeOrWithAnUnknownTypeIsRefused() {
        assertFailure(failureOf(withChildren("{\"a\": {}}")), "missing key 'type' in node a");
        assertFailure(failureOf(withChildren("{\"a\": {\"type\": \"root\"}}")), "type of node a is 'root' instead of 'literal' or 'argument'");
        assertFailure(failureOf(withChildren("{\"a\": {\"type\": 3}}")), "type of node a is not a string");
    }

    @Test
    void aLiteralWithAParserOrPropertiesIsRefused() {
        assertFailure(failureOf(withChildren("{\"a\": {\"type\": \"literal\", \"parser\": \"minecraft:entity\"}}")),
                "unknown key 'parser' in node a");
        assertFailure(failureOf(withChildren("{\"a\": {\"type\": \"literal\", \"properties\": {}}}")),
                "unknown key 'properties' in node a");
    }

    @Test
    void anArgumentWithoutAParserOrWithABadOneIsRefused() {
        assertFailure(failureOf(withChildren("{\"a\": {\"type\": \"argument\"}}")), "missing key 'parser' in node a");
        assertFailure(failureOf(withChildren("{\"a\": {\"type\": \"argument\", \"parser\": \"entity\"}}")), "Invalid parser id: 'entity'");
        assertFailure(failureOf(withChildren("{\"a\": {\"type\": \"argument\", \"parser\": 3}}")), "parser of node a is not a string");
    }

    @Test
    void anUnknownNodeKeyIsRefused() {
        assertFailure(failureOf(withChildren("{\"a\": {\"type\": \"literal\", \"hidden\": true}}")), "unknown key 'hidden' in node a");
    }

    @Test
    void anExecutableFlagThatIsNotABooleanIsRefused() {
        assertFailure(failureOf(withChildren("{\"a\": {\"type\": \"literal\", \"executable\": \"yes\"}}")),
                "executable of node a is not a boolean");
    }

    @Test
    void permissionsThatAreMalformedAreRefused() {
        assertFailure(failureOf(withChildren("{\"a\": {\"type\": \"literal\", \"permissions\": []}}")),
                "permissions of node a is not an object");
        assertFailure(failureOf(withChildren("{\"a\": {\"type\": \"literal\", \"permissions\": {\"type\": \"minecraft:require\"}}}")),
                "missing key 'permission' in permissions of node a");
        assertFailure(failureOf(withChildren("""
                {"a": {"type": "literal", "permissions": {"type": "minecraft:require",
                  "permission": {"type": "minecraft:command_level", "level": "admins", "extra": 1}}}}
                """)), "unknown key 'extra' in permission of permissions of node a");
        assertFailure(failureOf(withChildren("""
                {"a": {"type": "literal", "permissions": {"type": "minecraft:require",
                  "permission": {"type": "minecraft:command_level"}}}}
                """)), "missing key 'level' in permission of permissions of node a");
        assertFailure(failureOf(withChildren("""
                {"a": {"type": "literal", "permissions": {"type": "minecraft:require",
                  "permission": {"type": "minecraft:command_level", "level": "Admins"}}}}
                """)), "Invalid permission level: 'Admins'");
    }

    @Test
    void aRedirectThatIsMalformedOrPointsNowhereIsRefused() {
        assertFailure(failureOf(withChildren("{\"a\": {\"type\": \"literal\", \"redirect\": \"a\"}}")),
                "redirect of node a is not an array");
        assertFailure(failureOf(withChildren("{\"a\": {\"type\": \"literal\", \"redirect\": [3]}}")),
                "a name of redirect of node a is not a string");
        assertFailure(failureOf(withChildren("{\"a\": {\"type\": \"literal\", \"redirect\": []}}")),
                "A command path has at least one name");
        assertFailure(failureOf(withChildren("{\"a\": {\"type\": \"literal\", \"redirect\": [\"b\"]}}")),
                "The redirect of node a points to b, which is not a node");
    }

    @Test
    void propertiesThatAreMalformedAreRefused() {
        assertFailure(failureOf(withChildren("{\"a\": {\"type\": \"argument\", \"parser\": \"minecraft:x\", \"properties\": []}}")),
                "properties of node a is not an object");
        assertFailure(failureOf(withChildren("{\"a\": {\"type\": \"argument\", \"parser\": \"minecraft:x\", \"properties\": {\"color\": \"red\"}}}")),
                "unknown key 'color' in properties of node a");
        assertFailure(failureOf(withChildren("{\"a\": {\"type\": \"argument\", \"parser\": \"minecraft:x\", \"properties\": {\"min\": true}}}")),
                "min of properties of node a is neither a string nor a number");
    }

    @Test
    void anEmptyNodeNameIsRefused() {
        assertFailure(failureOf(withChildren("{\"\": {\"type\": \"literal\"}}")), "Invalid command node name: ''");
    }

    @Test
    void nodeNamesMayContainSymbolsUppercaseAndColons() {
        CommandTree read = read(withChildren("""
                {"*": {"type": "literal"}, "maxCount": {"type": "literal"}, ">=": {"type": "literal"}, "minecraft:overworld": {"type": "literal"}}
                """));
        assertEquals(List.of("*", ">=", "maxCount", "minecraft:overworld"),
                read.commands().keySet().stream().map(name -> name.value()).toList());
    }
}
