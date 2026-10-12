package tlm.dev.lectern.diff.packets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.direction;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.in;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.packet;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.packets;
import static tlm.dev.lectern.diff.fixtures.PacketFixtures.phase;
import static tlm.dev.lectern.diff.fixtures.RegistryFixtures.id;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;

import java.util.List;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.ids.IdShiftClassifier;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.model.PacketAdded;
import tlm.dev.lectern.diff.model.PacketDirectionAdded;
import tlm.dev.lectern.diff.model.PacketDirectionRemoved;
import tlm.dev.lectern.diff.model.PacketIdModified;
import tlm.dev.lectern.diff.model.PacketIdsShifted;
import tlm.dev.lectern.diff.model.PacketPhaseAdded;
import tlm.dev.lectern.diff.model.PacketPhaseRemoved;
import tlm.dev.lectern.diff.model.PacketRemoved;
import tlm.dev.lectern.diff.model.Packets;

class PacketDifferTest {

    private final PacketDiffer differ = new PacketDiffer(new IdShiftClassifier());

    private List<Change> diff(Packets from, Packets to) {
        List<Change> changes = List.copyOf(differ.diff(from, to));
        return new ChangeSet(release("1.0"), release("1.1"), changes).changes();
    }

    @Test
    void identicalPacketsGiveNoChange() {
        Packets same = packets(in("play", "clientbound").contiguous("demo:a", "demo:b"), in("play", "serverbound").contiguous("demo:c"));
        assertEquals(List.of(), diff(same, same));
    }

    @Test
    void anAddedPhaseIsOneChangeWithItsPacketCountOverEveryDirection() {
        Packets before = packets(in("play", "clientbound").contiguous("demo:a"));
        Packets after = packets(in("play", "clientbound").contiguous("demo:a"),
                in("login", "clientbound").contiguous("demo:b", "demo:c"), in("login", "serverbound").contiguous("demo:d"));
        assertEquals(List.of(new PacketPhaseAdded(phase("login"), 3)), diff(before, after));
    }

    @Test
    void aRemovedPhaseIsOneChangeWithItsPacketCount() {
        Packets before = packets(in("play", "clientbound").contiguous("demo:a"), in("status", "clientbound").contiguous("demo:b", "demo:c"));
        Packets after = packets(in("play", "clientbound").contiguous("demo:a"));
        assertEquals(List.of(new PacketPhaseRemoved(phase("status"), 2)), diff(before, after));
    }

    @Test
    void anAddedDirectionOfAnExistingPhaseIsOneChangeAndItsPacketsAreNotListed() {
        Packets before = packets(in("play", "clientbound").contiguous("demo:a"));
        Packets after = packets(in("play", "clientbound").contiguous("demo:a"), in("play", "serverbound").contiguous("demo:b", "demo:c"));
        assertEquals(List.of(new PacketDirectionAdded(phase("play"), direction("serverbound"), 2)), diff(before, after));
    }

    @Test
    void aRemovedDirectionOfAnExistingPhaseIsOneChangeWithItsPacketCount() {
        Packets before = packets(in("play", "clientbound").contiguous("demo:a"), in("play", "serverbound").contiguous("demo:b"));
        Packets after = packets(in("play", "clientbound").contiguous("demo:a"));
        assertEquals(List.of(new PacketDirectionRemoved(phase("play"), direction("serverbound"), 1)), diff(before, after));
    }

    @Test
    void packetsAddedAtTheEndAndRemovedAtTheEndShiftNothing() {
        Packets before = packets(in("play", "clientbound").contiguous("demo:a", "demo:b", "demo:c"));
        Packets after = packets(in("play", "clientbound").contiguous("demo:a", "demo:b", "demo:x"));
        assertEquals(
                List.of(new PacketAdded(phase("play"), direction("clientbound"), packet("demo:x"), id(2)),
                        new PacketRemoved(phase("play"), direction("clientbound"), packet("demo:c"), id(2))),
                diff(before, after));
    }

    @Test
    void aPacketInsertedInTheMiddleIsAddedAndShiftsTheLaterOnesAsOneAggregate() {
        Packets before = packets(in("play", "clientbound").contiguous("demo:a", "demo:b", "demo:c", "demo:d"));
        Packets after = packets(in("play", "clientbound").contiguous("demo:a", "demo:x", "demo:b", "demo:c", "demo:d"));
        assertEquals(
                List.of(new PacketAdded(phase("play"), direction("clientbound"), packet("demo:x"), id(1)),
                        new PacketIdsShifted(phase("play"), direction("clientbound"), 3, id(1))),
                diff(before, after));
    }

    @Test
    void theShiftIsComputedSeparatelyForEachPhaseAndDirection() {
        Packets before = packets(
                in("play", "clientbound").contiguous("demo:a", "demo:b"),
                in("play", "serverbound").contiguous("demo:a", "demo:b"),
                in("login", "clientbound").contiguous("demo:a", "demo:b"));
        Packets after = packets(
                in("play", "clientbound").contiguous("demo:x", "demo:a", "demo:b"),
                in("play", "serverbound").contiguous("demo:a", "demo:b"),
                in("login", "clientbound").contiguous("demo:y", "demo:a", "demo:b"));
        assertEquals(
                List.of(new PacketAdded(phase("login"), direction("clientbound"), packet("demo:y"), id(0)),
                        new PacketIdsShifted(phase("login"), direction("clientbound"), 2, id(0)),
                        new PacketAdded(phase("play"), direction("clientbound"), packet("demo:x"), id(0)),
                        new PacketIdsShifted(phase("play"), direction("clientbound"), 2, id(0))),
                diff(before, after));
    }

    @Test
    void twoPacketsSwappingTheirIdsAreRealChanges() {
        Packets before = packets(in("play", "serverbound").contiguous("demo:chat", "demo:jump"));
        Packets after = packets(in("play", "serverbound").contiguous("demo:jump", "demo:chat"));
        assertEquals(
                List.of(new PacketIdModified(phase("play"), direction("serverbound"), packet("demo:chat"), id(0), id(1)),
                        new PacketIdModified(phase("play"), direction("serverbound"), packet("demo:jump"), id(1), id(0))),
                diff(before, after));
    }

    @Test
    void aRealChangeNextToAShiftStaysAChange() {
        Packets before = packets(in("play", "clientbound").contiguous("demo:a", "demo:b", "demo:c", "demo:d"));
        Packets after = packets(in("play", "clientbound")
                .packet("demo:a", 0).packet("demo:x", 1).packet("demo:b", 2).packet("demo:c", 3).packet("demo:d", 9));
        assertEquals(
                List.of(new PacketAdded(phase("play"), direction("clientbound"), packet("demo:x"), id(1)),
                        new PacketIdModified(phase("play"), direction("clientbound"), packet("demo:d"), id(3), id(9)),
                        new PacketIdsShifted(phase("play"), direction("clientbound"), 2, id(1))),
                diff(before, after));
    }

    @Test
    void aNewPhaseOrDirectionDoesNotShiftTheOtherGroups() {
        Packets before = packets(in("play", "clientbound").contiguous("demo:a", "demo:b"));
        Packets after = packets(in("play", "clientbound").contiguous("demo:a", "demo:b"),
                in("configuration", "clientbound").contiguous("demo:c"), in("play", "serverbound").contiguous("demo:d"));
        assertEquals(
                List.of(new PacketPhaseAdded(phase("configuration"), 1),
                        new PacketDirectionAdded(phase("play"), direction("serverbound"), 1)),
                diff(before, after));
    }

    @Test
    void aRenameIsNeverInferredItIsARemovalAndAnAddition() {
        Packets before = packets(in("play", "clientbound").contiguous("demo:old_name"));
        Packets after = packets(in("play", "clientbound").contiguous("demo:new_name"));
        assertEquals(
                List.of(new PacketAdded(phase("play"), direction("clientbound"), packet("demo:new_name"), id(0)),
                        new PacketRemoved(phase("play"), direction("clientbound"), packet("demo:old_name"), id(0))),
                diff(before, after));
    }

    @Test
    void reversingTheComparisonMirrorsAdditionsAndRemovals() {
        Packets before = packets(in("play", "clientbound").contiguous("demo:a"));
        Packets after = packets(in("play", "clientbound").contiguous("demo:a", "demo:b"));
        assertEquals(List.of(new PacketAdded(phase("play"), direction("clientbound"), packet("demo:b"), id(1))), diff(before, after));
        assertEquals(List.of(new PacketRemoved(phase("play"), direction("clientbound"), packet("demo:b"), id(1))), diff(after, before));
    }
}
