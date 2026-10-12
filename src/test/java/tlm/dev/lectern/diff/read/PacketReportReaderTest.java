package tlm.dev.lectern.diff.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.group;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.packet;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.phase;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.id;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.fixtures.InMemoryVersionTree;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.model.Packets;

class PacketReportReaderTest {

    private final PacketReportReader reader = new PacketReportReader(new JsonReader());

    private static VersionTree tree(String packetsJson) {
        return new InMemoryVersionTree(release("9.1"), Map.of("reports/packets.json", packetsJson));
    }

    private Packets read(String packetsJson) {
        return reader.read(tree(packetsJson)).orElseThrow();
    }

    private String failureOf(String packetsJson) {
        return assertThrows(VersionReadException.class, () -> reader.read(tree(packetsJson))).getMessage();
    }

    private static void assertFailure(String message, String fragment) {
        assertTrue(message.startsWith("Version 9.1, reports/packets.json: "), message);
        assertTrue(message.contains(fragment), message);
    }

    @Test
    void readsPacketsByPhaseAndDirection() {
        Packets packets = read("""
                {
                  "play": {
                    "clientbound": {"demo:ping": {"protocol_id": 0}, "demo:pong": {"protocol_id": 1}},
                    "serverbound": {"demo:chat": {"protocol_id": 0}}
                  },
                  "handshake": {"serverbound": {"demo:hello": {"protocol_id": 0}}}
                }
                """);
        assertEquals(List.of(group("handshake", "serverbound"), group("play", "clientbound"), group("play", "serverbound")),
                List.copyOf(packets.groups().keySet()));
        assertEquals(id(1), packets.groups().get(group("play", "clientbound")).get(packet("demo:pong")));
        assertEquals(List.of(phase("handshake"), phase("play")), List.copyOf(packets.phases()));
    }

    @Test
    void theOrderOfPhasesDirectionsAndPacketsDoesNotMatter() {
        Packets first = read("""
                {"play": {"clientbound": {"demo:a": {"protocol_id": 0}, "demo:b": {"protocol_id": 1}},
                          "serverbound": {"demo:c": {"protocol_id": 0}}}}
                """);
        Packets second = read("""
                {"play": {"serverbound": {"demo:c": {"protocol_id": 0}},
                          "clientbound": {"demo:b": {"protocol_id": 1}, "demo:a": {"protocol_id": 0}}}}
                """);
        assertEquals(first, second);
    }

    @Test
    void aDirectionWithoutPacketsIsAllowed() {
        Packets packets = read("{\"play\": {\"clientbound\": {}}}");
        assertEquals(Map.of(), packets.groups().get(group("play", "clientbound")));
    }

    @Test
    void aMissingReportGivesNoPacketsInsteadOfAnError() {
        assertEquals(Optional.empty(), reader.read(new InMemoryVersionTree(release("9.1"), Map.of())));
    }

    @Test
    void invalidJsonAndShapesAreRefusedWithTheVersionAndTheFile() {
        assertFailure(failureOf("{\"play\": "), "not valid JSON");
        assertFailure(failureOf("[]"), "the document is not an object");
        assertFailure(failureOf("{\"play\": 3}"), "phase play is not an object");
        assertFailure(failureOf("{\"play\": {\"clientbound\": []}}"), "packets of play/clientbound is not an object");
        assertFailure(failureOf("{\"play\": {\"clientbound\": {\"demo:a\": 3}}}"), "packet demo:a of play/clientbound is not an object");
    }

    @Test
    void aPhaseWithoutAnyDirectionIsRefused() {
        assertFailure(failureOf("{\"play\": {}}"), "phase play has no direction");
    }

    @Test
    void anUnknownDirectionIsRefused() {
        assertFailure(failureOf("{\"play\": {\"both\": {}}}"), "unknown key 'both' in phase play");
    }

    @Test
    void anUnknownKeyInAPacketIsRefused() {
        assertFailure(failureOf("{\"play\": {\"clientbound\": {\"demo:a\": {\"protocol_id\": 0, \"state\": 2}}}}"),
                "unknown key 'state' in packet demo:a of play/clientbound");
    }

    @Test
    void aPacketWithoutAnIdOrWithAnIdThatIsNotAWholeNonNegativeNumberIsRefused() {
        assertFailure(failureOf("{\"play\": {\"clientbound\": {\"demo:a\": {}}}}"),
                "missing key 'protocol_id' in packet demo:a of play/clientbound");
        for (String bad : List.of("\"3\"", "1.0", "1e2", "-1", "99999999999", "true", "null")) {
            assertFailure(failureOf("{\"play\": {\"clientbound\": {\"demo:a\": {\"protocol_id\": " + bad + "}}}}"),
                    "protocol_id of packet demo:a of play/clientbound is not");
        }
    }

    @Test
    void twoPacketsOfAGroupWithTheSameIdAreRefused() {
        assertFailure(failureOf("""
                {"play": {"clientbound": {"demo:a": {"protocol_id": 2}, "demo:b": {"protocol_id": 2}}}}
                """), "Protocol id 2 is used twice in play/clientbound");
    }

    @Test
    void namesThatBreakTheSyntaxAreRefused() {
        assertFailure(failureOf("{\"Play\": {\"clientbound\": {}}}"), "Invalid packet phase: 'Play'");
        assertFailure(failureOf("{\"play\": {\"clientbound\": {\"ping\": {\"protocol_id\": 0}}}}"), "Invalid packet name: 'ping'");
    }
}
