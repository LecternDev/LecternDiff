package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record RegistryEntryAdded(RegistryKey registry, EntryKey entry, ProtocolId id) implements RegistryChange {

    public RegistryEntryAdded {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(entry, "entry");
        Objects.requireNonNull(id, "id");
    }
}
