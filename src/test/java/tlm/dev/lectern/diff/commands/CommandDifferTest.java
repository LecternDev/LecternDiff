package tlm.dev.lectern.diff.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.argument;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.command;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.literal;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.name;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.number;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.path;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.properties;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.requires;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.text;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.tree;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.model.CommandAdded;
import tlm.dev.lectern.diff.model.CommandExecutableModified;
import tlm.dev.lectern.diff.model.CommandNodeAdded;
import tlm.dev.lectern.diff.model.CommandNodeRemoved;
import tlm.dev.lectern.diff.model.CommandParserModified;
import tlm.dev.lectern.diff.model.CommandPermissionsModified;
import tlm.dev.lectern.diff.model.CommandPropertiesModified;
import tlm.dev.lectern.diff.model.CommandRedirectModified;
import tlm.dev.lectern.diff.model.CommandRemoved;
import tlm.dev.lectern.diff.model.CommandTree;
import tlm.dev.lectern.diff.model.NodeKind;
import tlm.dev.lectern.diff.model.ParserId;
import tlm.dev.lectern.diff.model.ParserProperties;

class CommandDifferTest {

    private final CommandDiffer differ = new CommandDiffer();

    private List<Change> diff(CommandTree from, CommandTree to) {
        return new ChangeSet(release("1.0"), release("1.1"), List.copyOf(differ.diff(from, to))).changes();
    }

    @Test
    void identicalTreesGiveNoChange() {
        CommandTree same = tree(command("gadget", literal().executable().child("size", argument("brigadier:integer").executable())));
        assertEquals(List.of(), diff(same, same));
    }

    @Test
    void anAddedTopLevelNodeIsOneChangeWithItsWholeSubtreeCounted() {
        CommandTree before = tree(command("gadget", literal()));
        CommandTree after = tree(command("gadget", literal()),
                command("lamp", literal().child("on", literal().executable()).child("level", argument("brigadier:integer").executable())));
        assertEquals(List.of(new CommandAdded(name("lamp"), NodeKind.LITERAL, 3)), diff(before, after));
    }

    @Test
    void aRemovedTopLevelNodeIsOneChangeWithItsWholeSubtreeCounted() {
        CommandTree before = tree(command("gadget", literal()), command("lamp", literal().child("on", literal().child("now", literal()))));
        CommandTree after = tree(command("gadget", literal()));
        assertEquals(List.of(new CommandRemoved(name("lamp"), NodeKind.LITERAL, 3)), diff(before, after));
    }

    @Test
    void anAddedSubtreeBelowAnExistingNodeIsOneChangeAtItsRootAndNeverOnePerDescendant() {
        CommandTree before = tree(command("gadget", literal().child("size", literal())));
        CommandTree after = tree(command("gadget", literal().child("size", literal()).child("mode",
                argument("minecraft:entity").child("then", literal().child("deep", literal().executable())))));
        assertEquals(List.of(new CommandNodeAdded(path("gadget", "mode"), NodeKind.ARGUMENT, 3)), diff(before, after));
    }

    @Test
    void aRemovedSubtreeBelowAnExistingNodeIsOneChangeAtItsRootAndNeverOnePerDescendant() {
        CommandTree before = tree(command("gadget", literal().child("size", literal()).child("mode",
                literal().child("then", literal()).child("else", literal()))));
        CommandTree after = tree(command("gadget", literal().child("size", literal())));
        assertEquals(List.of(new CommandNodeRemoved(path("gadget", "mode"), NodeKind.LITERAL, 3)), diff(before, after));
    }

    @Test
    void aNodeBelowAnAddedNodeIsNotReportedOnItsOwn() {
        CommandTree before = tree(command("gadget", literal()));
        CommandTree after = tree(command("gadget", literal().child("a", literal().child("b", literal().child("c", literal())))));
        assertEquals(List.of(new CommandNodeAdded(path("gadget", "a"), NodeKind.LITERAL, 3)), diff(before, after));
    }

    @Test
    void aChangedParserIsOneChangeWithBothParsers() {
        CommandTree before = tree(command("gadget", literal().child("target", argument("minecraft:entity"))));
        CommandTree after = tree(command("gadget", literal().child("target", argument("minecraft:game_profile"))));
        assertEquals(List.of(new CommandParserModified(path("gadget", "target"), new ParserId("minecraft:entity"),
                new ParserId("minecraft:game_profile"))), diff(before, after));
    }

    @Test
    void propertiesGainedChangedAndLostAreChangesWithNullForNone() {
        CommandTree none = tree(command("gadget", literal().child("steps", argument("brigadier:integer"))));
        CommandTree bounded = tree(command("gadget", literal().child("steps", argument("brigadier:integer").number("min", "0").number("max", "3"))));
        CommandTree widened = tree(command("gadget", literal().child("steps", argument("brigadier:integer").number("min", "0").number("max", "9"))));
        Optional<ParserProperties> zeroToThree =
                Optional.of(properties(Map.of("min", number("0"), "max", number("3"))));
        Optional<ParserProperties> zeroToNine =
                Optional.of(properties(Map.of("min", number("0"), "max", number("9"))));
        assertEquals(List.of(new CommandPropertiesModified(path("gadget", "steps"), Optional.empty(), zeroToThree)), diff(none, bounded));
        assertEquals(List.of(new CommandPropertiesModified(path("gadget", "steps"), zeroToThree, zeroToNine)), diff(bounded, widened));
        assertEquals(List.of(new CommandPropertiesModified(path("gadget", "steps"), zeroToNine, Optional.empty())), diff(widened, none));
    }

    @Test
    void aPropertyWrittenWithAnotherTextIsAChange() {
        CommandTree before = tree(command("gadget", argument("brigadier:float").number("min", "0")));
        CommandTree after = tree(command("gadget", argument("brigadier:float").number("min", "0.0")));
        assertEquals(1, diff(before, after).size());
    }

    @Test
    void aTextPropertyAndANumberPropertyWithTheSameSpellingAreDifferent() {
        CommandTree before = tree(command("gadget", argument("brigadier:string").text("type", "word")));
        CommandTree after = tree(command("gadget", argument("brigadier:string").text("type", "phrase")));
        assertEquals(List.of(new CommandPropertiesModified(path("gadget"),
                Optional.of(properties(Map.of("type", text("word")))), Optional.of(properties(Map.of("type", text("phrase")))))),
                diff(before, after));
    }

    @Test
    void anExecutableFlagThatTurnsOnOrOffIsAChange() {
        CommandTree before = tree(command("gadget", literal().child("go", literal()).child("stop", literal().executable())));
        CommandTree after = tree(command("gadget", literal().child("go", literal().executable()).child("stop", literal())));
        assertEquals(List.of(new CommandExecutableModified(path("gadget", "go"), false, true),
                new CommandExecutableModified(path("gadget", "stop"), true, false)), diff(before, after));
    }

    @Test
    void permissionsGainedChangedAndLostAreChanges() {
        CommandTree open = tree(command("gadget", literal()));
        CommandTree gamemasters = tree(command("gadget", literal().requires("gamemasters")));
        CommandTree admins = tree(command("gadget", literal().requires("admins")));
        assertEquals(List.of(new CommandPermissionsModified(path("gadget"), Optional.empty(), Optional.of(requires("gamemasters")))),
                diff(open, gamemasters));
        assertEquals(List.of(new CommandPermissionsModified(path("gadget"), Optional.of(requires("gamemasters")), Optional.of(requires("admins")))),
                diff(gamemasters, admins));
        assertEquals(List.of(new CommandPermissionsModified(path("gadget"), Optional.of(requires("admins")), Optional.empty())),
                diff(admins, open));
    }

    @Test
    void aRedirectGainedChangedAndLostIsAChange() {
        CommandTree plain = tree(command("gadget", literal().child("again", literal())));
        CommandTree toGadget = tree(command("gadget", literal().child("again", literal().redirect("gadget"))));
        CommandTree toAgain = tree(command("gadget", literal().child("again", literal().redirect("gadget", "again"))));
        assertEquals(List.of(new CommandRedirectModified(path("gadget", "again"), Optional.empty(), Optional.of(path("gadget")))),
                diff(plain, toGadget));
        assertEquals(List.of(new CommandRedirectModified(path("gadget", "again"), Optional.of(path("gadget")),
                Optional.of(path("gadget", "again")))), diff(toGadget, toAgain));
        assertEquals(List.of(new CommandRedirectModified(path("gadget", "again"), Optional.of(path("gadget", "again")), Optional.empty())),
                diff(toAgain, plain));
    }

    @Test
    void theOrderOfTheChildrenDoesNotMatter() {
        CommandTree first = tree(command("gadget", literal().child("a", literal()).child("b", literal()).child("c", literal())));
        CommandTree second = tree(command("gadget", literal().child("c", literal()).child("a", literal()).child("b", literal())));
        assertEquals(List.of(), diff(first, second));
    }

    @Test
    void aNodeThatChangesKindAtTheSamePathIsTheOldSubtreeRemovedAndTheNewOneAdded() {
        CommandTree before = tree(command("gadget", literal().child("size", literal().child("big", literal()))));
        CommandTree after = tree(command("gadget", literal().child("size", argument("brigadier:integer"))));
        assertEquals(List.of(new CommandNodeAdded(path("gadget", "size"), NodeKind.ARGUMENT, 1),
                new CommandNodeRemoved(path("gadget", "size"), NodeKind.LITERAL, 2)), diff(before, after));
    }

    @Test
    void aTopLevelNodeThatChangesKindIsARemovalAndAnAdditionOfTheSameName() {
        CommandTree before = tree(command("gadget", literal()));
        CommandTree after = tree(command("gadget", argument("brigadier:integer")));
        assertEquals(List.of(new CommandAdded(name("gadget"), NodeKind.ARGUMENT, 1), new CommandRemoved(name("gadget"), NodeKind.LITERAL, 1)),
                diff(before, after));
    }

    @Test
    void aRenamedNodeIsARemovalAndAnAdditionAndNeverARename() {
        CommandTree before = tree(command("gadget", literal().child("old", literal().executable())));
        CommandTree after = tree(command("gadget", literal().child("new", literal().executable())));
        assertEquals(List.of(new CommandNodeAdded(path("gadget", "new"), NodeKind.LITERAL, 1),
                new CommandNodeRemoved(path("gadget", "old"), NodeKind.LITERAL, 1)), diff(before, after));
    }

    @Test
    void severalAttributesOfOneNodeGiveOneChangeEach() {
        CommandTree before = tree(command("gadget", argument("minecraft:entity").requires("gamemasters")));
        CommandTree after = tree(command("gadget", argument("minecraft:vec3").executable()));
        assertEquals(List.of(new CommandParserModified(path("gadget"), new ParserId("minecraft:entity"), new ParserId("minecraft:vec3")),
                new CommandExecutableModified(path("gadget"), false, true),
                new CommandPermissionsModified(path("gadget"), Optional.of(requires("gamemasters")), Optional.empty())), diff(before, after));
    }

    @Test
    void changesInsideAnUnchangedParentAreReportedAtTheirOwnPath() {
        CommandTree before = tree(command("gadget", literal().child("mode", literal().child("fast", argument("brigadier:integer")))));
        CommandTree after = tree(command("gadget", literal().child("mode", literal().child("fast", argument("brigadier:long")))));
        assertEquals(List.of(new CommandParserModified(path("gadget", "mode", "fast"), new ParserId("brigadier:integer"),
                new ParserId("brigadier:long"))), diff(before, after));
    }
}
