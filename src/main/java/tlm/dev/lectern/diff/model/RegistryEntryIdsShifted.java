package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record RegistryEntryIdsShifted(RegistryKey registry, int count, ProtocolId firstId) implements RegistryChange {

    public RegistryEntryIdsShifted {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(firstId, "firstId");
        if (count < 1) {
            throw new IllegalArgumentException("A shift touches at least one entry: " + count);
        }
    }
}
