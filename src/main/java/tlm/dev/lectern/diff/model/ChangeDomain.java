package tlm.dev.lectern.diff.model;

import java.util.List;

public sealed interface ChangeDomain permits ChangeDomain.RegistryDomain, ChangeDomain.BlockDomain {

    ChangeDomain REGISTRIES = new RegistryDomain();
    ChangeDomain BLOCKS = new BlockDomain();
    List<ChangeDomain> ALL = List.of(REGISTRIES, BLOCKS);

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
}
