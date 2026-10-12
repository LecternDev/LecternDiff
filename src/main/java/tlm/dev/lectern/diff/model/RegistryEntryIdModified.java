package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record RegistryEntryIdModified(RegistryKey registry, EntryKey entry, ProtocolId oldId, ProtocolId newId)
        implements RegistryChange {

    public RegistryEntryIdModified {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(entry, "entry");
        Objects.requireNonNull(oldId, "oldId");
        Objects.requireNonNull(newId, "newId");
        if (oldId.equals(newId)) {
            throw new IllegalArgumentException("The id of entry " + entry + " of registry " + registry + " did not change");
        }
    }
}
