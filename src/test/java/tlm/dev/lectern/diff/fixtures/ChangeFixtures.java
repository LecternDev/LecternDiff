package tlm.dev.lectern.diff.fixtures;

import static tlm.dev.lectern.diff.fixtures.BlockFixtures.blockKey;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.property;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.state;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.stateId;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.value;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.values;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.name;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.number;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.path;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.properties;
import static tlm.dev.lectern.diff.fixtures.CommandFixtures.requires;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.direction;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.packet;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.phase;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.entry;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.id;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.key;

import java.util.List;
import java.util.Map;
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
import tlm.dev.lectern.diff.model.CommandAdded;
import tlm.dev.lectern.diff.model.CommandExecutableModified;
import tlm.dev.lectern.diff.model.CommandNodeAdded;
import tlm.dev.lectern.diff.model.CommandNodeRemoved;
import tlm.dev.lectern.diff.model.CommandParserModified;
import tlm.dev.lectern.diff.model.CommandPermissionsModified;
import tlm.dev.lectern.diff.model.CommandPropertiesModified;
import tlm.dev.lectern.diff.model.CommandRedirectModified;
import tlm.dev.lectern.diff.model.CommandRemoved;
import tlm.dev.lectern.diff.model.NodeKind;
import tlm.dev.lectern.diff.model.PacketAdded;
import tlm.dev.lectern.diff.model.PacketDirectionAdded;
import tlm.dev.lectern.diff.model.PacketDirectionRemoved;
import tlm.dev.lectern.diff.model.PacketIdModified;
import tlm.dev.lectern.diff.model.PacketIdsShifted;
import tlm.dev.lectern.diff.model.PacketPhaseAdded;
import tlm.dev.lectern.diff.model.PacketPhaseRemoved;
import tlm.dev.lectern.diff.model.PacketRemoved;
import tlm.dev.lectern.diff.model.ParserId;
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
                new BlockPropertyAdded(blockKey("demo:crate"), property("facing"), values("north", "south"), value("north")),
                new BlockPropertyRemoved(blockKey("demo:crate"), property("lit"), values("true", "false"), value("true")),
                new BlockPropertyValuesAdded(blockKey("demo:door"), property("half"), values("upper")),
                new BlockPropertyValuesRemoved(blockKey("demo:door"), property("hinge"), values("middle")),
                new BlockDefaultStateModified(blockKey("demo:lamp"), state("lit=false"), state("lit=true")),
                new BlockStateIdsModified(blockKey("demo:lever"), 2, stateId(10), stateId(14)),
                new PacketPhaseAdded(phase("login"), 6),
                new PacketPhaseRemoved(phase("status"), 4),
                new PacketDirectionAdded(phase("configuration"), direction("serverbound"), 3),
                new PacketDirectionRemoved(phase("handshake"), direction("clientbound"), 2),
                new PacketAdded(phase("play"), direction("clientbound"), packet("demo:ping"), id(7)),
                new PacketRemoved(phase("play"), direction("clientbound"), packet("demo:pong"), id(9)),
                new PacketIdModified(phase("play"), direction("serverbound"), packet("demo:chat"), id(3), id(5)),
                new PacketIdsShifted(phase("play"), direction("clientbound"), 12, id(4)),
                new CommandAdded(name("dialog"), NodeKind.LITERAL, 6),
                new CommandRemoved(name("legacy"), NodeKind.LITERAL, 2),
                new CommandNodeAdded(path("fetch", "profile"), NodeKind.ARGUMENT, 4),
                new CommandNodeRemoved(path("fetch", "old"), NodeKind.LITERAL, 1),
                new CommandParserModified(path("effect", "give", "effect"), new ParserId("minecraft:mob_effect"),
                        new ParserId("minecraft:resource")),
                new CommandPropertiesModified(path("test", "run", "rotationSteps"), Optional.empty(),
                        Optional.of(properties(Map.of("min", number("0"), "max", number("3"))))),
                new CommandExecutableModified(path("kill"), false, true),
                new CommandPermissionsModified(path("op"), Optional.empty(), Optional.of(requires("admins"))),
                new CommandRedirectModified(path("execute", "run"), Optional.of(path("execute")), Optional.empty()));
    }
}
