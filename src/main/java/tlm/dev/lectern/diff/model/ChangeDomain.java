package tlm.dev.lectern.diff.model;

import java.util.List;

public sealed interface ChangeDomain permits ChangeDomain.RegistryDomain, ChangeDomain.BlockDomain, ChangeDomain.PacketDomain {

    ChangeDomain REGISTRIES = new RegistryDomain();
    ChangeDomain BLOCKS = new BlockDomain();
    ChangeDomain PACKETS = new PacketDomain();
    List<ChangeDomain> ALL = List.of(REGISTRIES, BLOCKS, PACKETS);

    String label();

    int rank();

    record RegistryDomain() implements ChangeDomain {

        @Override
        public String label() {
            return "registries";
        }

        @Override
        public int rank() {
            return 0;
        }
    }

    record BlockDomain() implements ChangeDomain {

        @Override
        public String label() {
            return "blocks";
        }

        @Override
        public int rank() {
            return 1;
        }
    }

    record PacketDomain() implements ChangeDomain {

        @Override
        public String label() {
            return "packets";
        }

        @Override
        public int rank() {
            return 2;
        }
    }
}
