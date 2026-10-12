package tlm.dev.lectern.diff.model;

import java.util.List;
import java.util.Optional;

public sealed interface PacketDirection extends Comparable<PacketDirection>
        permits PacketDirection.Clientbound, PacketDirection.Serverbound {

    PacketDirection CLIENTBOUND = new Clientbound();
    PacketDirection SERVERBOUND = new Serverbound();
    List<PacketDirection> ALL = List.of(CLIENTBOUND, SERVERBOUND);

    String label();

    static Optional<PacketDirection> ofLabel(String label) {
        return ALL.stream().filter(direction -> direction.label().equals(label)).findFirst();
    }

    @Override
    default int compareTo(PacketDirection other) {
        return label().compareTo(other.label());
    }

    record Clientbound() implements PacketDirection {

        @Override
        public String label() {
            return "clientbound";
        }
    }

    record Serverbound() implements PacketDirection {

        @Override
        public String label() {
            return "serverbound";
        }
    }
}
