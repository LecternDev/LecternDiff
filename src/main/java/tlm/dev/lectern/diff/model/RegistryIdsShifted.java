package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record RegistryIdsShifted(int count, ProtocolId firstId) implements RegistryChange {

    public RegistryIdsShifted {
        Objects.requireNonNull(firstId, "firstId");
        if (count < 1) {
            throw new IllegalArgumentException("A shift touches at least one registry: " + count);
        }
    }
}
