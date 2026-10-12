package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record RegistryIdModified(RegistryKey registry, ProtocolId oldId, ProtocolId newId) implements RegistryChange {

    public RegistryIdModified {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(oldId, "oldId");
        Objects.requireNonNull(newId, "newId");
        if (oldId.equals(newId)) {
            throw new IllegalArgumentException("The id of registry " + registry + " did not change");
        }
    }
}
