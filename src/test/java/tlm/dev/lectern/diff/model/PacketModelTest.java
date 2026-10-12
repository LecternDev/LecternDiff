package tlm.dev.lectern.diff.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.group;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.in;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.packets;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.phase;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PacketModelTest {

    @Test
    void phasesAcceptLowercaseDigitsAndUnderscores() {
        for (String valid : List.of("play", "configuration", "phase_2")) {
            assertDoesNotThrow(() -> new PacketPhase(valid), valid);
        }
        for (String invalid : List.of("", "Play", "pl ay", "play-2", "minecraft:play")) {
            assertThrows(IllegalArgumentException.class, () -> new PacketPhase(invalid), "'" + invalid + "'");
        }
    }

    @Test
    void packetNamesAreNamespacedLikeEveryOtherKey() {
        assertDoesNotThrow(() -> new PacketName("minecraft:keep_alive"));
        assertThrows(IllegalArgumentException.class, () -> new PacketName("keep_alive"));
    }

    @Test
    void directionsAreClosedAndReadBackFromTheirLabel() {
        assertEquals(List.of("clientbound", "serverbound"), PacketDirection.ALL.stream().map(PacketDirection::label).toList());
        assertEquals(Optional.of(PacketDirection.SERVERBOUND), PacketDirection.ofLabel("serverbound"));
        assertEquals(Optional.empty(), PacketDirection.ofLabel("both"));
        assertEquals(
                Set.of(PacketDirection.class.getPermittedSubclasses()),
                PacketDirection.ALL.stream().<Class<?>>map(Object::getClass).collect(Collectors.toSet()));
    }

    @Test
    void groupsAreOrderedByPhaseThenDirection() {
        List<PacketGroup> sorted = List.of(group("play", "serverbound"), group("login", "serverbound"),
                group("play", "clientbound"), group("configuration", "clientbound")).stream().sorted().toList();
        assertEquals(List.of(group("configuration", "clientbound"), group("login", "serverbound"),
                group("play", "clientbound"), group("play", "serverbound")), sorted);
    }

    @Test
    void twoPacketsOfAGroupCannotShareAnId() {
        assertThrows(IllegalArgumentException.class,
                () -> packets(in("play", "clientbound").packet("demo:a", 1).packet("demo:b", 1)));
    }

    @Test
    void theSameIdMayBeUsedInTwoGroups() {
        assertDoesNotThrow(() -> packets(in("play", "clientbound").packet("demo:a", 0), in("play", "serverbound").packet("demo:b", 0)));
    }

    @Test
    void thePhasesAndTheirPacketCountsCoverEveryDirection() {
        Packets all = packets(
                in("play", "clientbound").contiguous("demo:a", "demo:b"),
                in("play", "serverbound").contiguous("demo:c"),
                in("status", "clientbound").contiguous("demo:d"));
        assertEquals(List.of(phase("play"), phase("status")), List.copyOf(all.phases()));
        assertEquals(3, all.packetCount(phase("play")));
        assertEquals(1, all.packetCount(phase("status")));
        assertEquals(0, all.packetCount(phase("login")));
    }
}
