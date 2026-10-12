package tlm.dev.lectern.diff.model;

import java.util.Objects;
import java.util.Optional;

public record RegistryDefaultModified(RegistryKey registry, Optional<EntryKey> oldDefault, Optional<EntryKey> newDefault)
        implements RegistryChange {

    public RegistryDefaultModified {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(oldDefault, "oldDefault");
        Objects.requireNonNull(newDefault, "newDefault");
        if (oldDefault.equals(newDefault)) {
            throw new IllegalArgumentException("The default of registry " + registry + " did not change");
        }
    }
}
