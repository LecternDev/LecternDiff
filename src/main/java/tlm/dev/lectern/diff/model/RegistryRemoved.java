package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record RegistryRemoved(RegistryKey registry, ProtocolId id, int entryCount) implements RegistryChange {

    public RegistryRemoved {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(id, "id");
        if (entryCount < 0) {
            throw new IllegalArgumentException("Entry count cannot be negative: " + entryCount);
        }
    }
}
