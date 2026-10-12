package tlm.dev.lectern.diff.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.argument;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.command;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.literal;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.name;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.path;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.tree;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CommandModelTest {

    @Test
    void nodeNamesAcceptAnythingPrintableAndRefuseEmptyOrControlCharacters() {
        for (String valid : List.of("say", "rotationSteps", "*", "<", ">=", "a-b", "minecraft:overworld")) {
            assertDoesNotThrow(() -> new CommandNodeName(valid), valid);
        }
        for (String invalid : List.of("", "a\nb", "a\tb")) {
            assertThrows(IllegalArgumentException.class, () -> new CommandNodeName(invalid), "'" + invalid + "'");
        }
    }

    @Test
    void pathsAreComparedNameByNameAndAPrefixComesFirst() {
        List<CommandPath> sorted = List.of(path("b"), path("a", "b"), path("ab"), path("a"), path("a", "a")).stream().sorted().toList();
        assertEquals(List.of(path("a"), path("a", "a"), path("a", "b"), path("ab"), path("b")), sorted);
    }

    @Test
    void aPathHasAtLeastOneNameAndGrowsByChild() {
        assertThrows(IllegalArgumentException.class, () -> new CommandPath(List.of()));
        assertEquals(path("execute", "run"), CommandPath.of(name("execute")).child(name("run")));
        assertEquals("execute > run", path("execute", "run").toString());
    }

    @Test
    void nodeKindsAreClosedAndReadBackFromTheirLabel() {
        assertEquals(Optional.of(NodeKind.ARGUMENT), NodeKind.ofLabel("argument"));
        assertEquals(Optional.empty(), NodeKind.ofLabel("root"));
        assertEquals(Set.of(NodeKind.class.getPermittedSubclasses()),
                NodeKind.ALL.stream().<Class<?>>map(Object::getClass).collect(Collectors.toSet()));
    }

    @Test
    void parserIdsAreNamespaced() {
        assertDoesNotThrow(() -> new ParserId("minecraft:entity"));
        assertDoesNotThrow(() -> new ParserId("brigadier:bool"));
        assertThrows(IllegalArgumentException.class, () -> new ParserId("entity"));
    }

    @Test
    void parserPropertyNumbersKeepTheirWrittenTextAndRefuseNonNumbers() {
        for (String valid : List.of("0", "-1", "0.0", "-5.9999968E7", "1.0E-5", "123456789012345678901234567890")) {
            assertEquals(valid, new ParserPropertyValue.Number(valid).text());
        }
        for (String invalid : List.of("", "abc", "01", "1.", ".5", "+1", "1e")) {
            assertThrows(IllegalArgumentException.class, () -> new ParserPropertyValue.Number(invalid), "'" + invalid + "'");
        }
        assertEquals(false, new ParserPropertyValue.Number("0").equals(new ParserPropertyValue.Number("0.0")));
    }

    @Test
    void parserPropertiesAreSortedByNameAndNamesAreSnakeCase() {
        ParserProperties properties = new ParserProperties(Map.of("min", new ParserPropertyValue.Number("0"),
                "amount", new ParserPropertyValue.Text("multiple")));
        assertEquals(List.of("amount", "min"), List.copyOf(properties.values().keySet()));
        assertThrows(IllegalArgumentException.class,
                () -> new ParserProperties(Map.of("Min", new ParserPropertyValue.Number("0"))));
    }

    @Test
    void permissionRulesNeedNamespacedTypesAndASnakeCaseLevel() {
        assertDoesNotThrow(() -> new PermissionRule("minecraft:require", "minecraft:command_level", "gamemasters"));
        assertThrows(IllegalArgumentException.class, () -> new PermissionRule("require", "minecraft:command_level", "admins"));
        assertThrows(IllegalArgumentException.class, () -> new PermissionRule("minecraft:require", "level", "admins"));
        assertThrows(IllegalArgumentException.class, () -> new PermissionRule("minecraft:require", "minecraft:command_level", "Admins"));
    }

    @Test
    void aTreeFindsNodesByPathAndCountsEveryNode() {
        CommandTree commands = tree(command("execute", literal()
                .child("as", literal().child("targets", argument("minecraft:entity").child("run", literal().executable())))));
        assertEquals(4, commands.nodeCount());
        assertEquals(Optional.of(NodeKind.ARGUMENT), commands.node(path("execute", "as", "targets")).map(CommandNode::kind));
        assertEquals(Optional.empty(), commands.node(path("execute", "missing")));
        assertEquals(2, commands.node(path("execute", "as", "targets")).orElseThrow().size() );
    }

    @Test
    void aRedirectMustPointToANodeOfTheTree() {
        assertDoesNotThrow(() -> tree(command("execute", literal().child("as", literal().redirect("execute")))));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> tree(command("execute", literal().child("as", literal().redirect("nothing")))));
        assertEquals("The redirect of node execute > as points to nothing, which is not a node", failure.getMessage());
    }

    @Test
    void childrenAreKeptInNameOrderWhateverTheInsertionOrder() {
        CommandNode node = literal().child("b", literal()).child("a", literal()).child("c", literal()).build();
        assertEquals(List.of(name("a"), name("b"), name("c")), List.copyOf(node.children().keySet()));
    }
}
