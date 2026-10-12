package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record VersionRef(VersionId id, VersionKind kind) {

    public VersionRef {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
    }
}
