package tlm.dev.lectern.diff.model;

import java.util.List;
import java.util.Objects;

public record ChangeSet(VersionRef from, VersionRef to, List<Change> changes) {

    public static final int SCHEMA_VERSION = 1;

    public ChangeSet {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        changes = changes.stream().sorted(ChangeOrder.COMPARATOR).toList();
    }

    public int schemaVersion() {
        return SCHEMA_VERSION;
    }
}
