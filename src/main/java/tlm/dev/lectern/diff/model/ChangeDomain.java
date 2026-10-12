package tlm.dev.lectern.diff.model;

import java.util.List;

public sealed interface ChangeDomain permits ChangeDomain.RegistryDomain, ChangeDomain.BlockDomain, ChangeDomain.PacketDomain,
        ChangeDomain.CommandDomain {

    ChangeDomain REGISTRIES = new RegistryDomain();
    ChangeDomain BLOCKS = new BlockDomain();
    ChangeDomain PACKETS = new PacketDomain();
    ChangeDomain COMMANDS = new CommandDomain();
    List<ChangeDomain> ALL = List.of(REGISTRIES, BLOCKS, PACKETS, COMMANDS);

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

    record CommandDomain() implements ChangeDomain {

        @Override
        public String label() {
            return "commands";
        }

        @Override
        public int rank() {
            return 3;
        }
    }
}
