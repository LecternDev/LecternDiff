package tlm.dev.lectern.diff.model;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ChangeSet(VersionRef from, VersionRef to, List<SkippedDomain> skipped, List<Change> changes) {

    public static final int SCHEMA_VERSION = 1;

    public ChangeSet {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Set<ChangeDomain> seen = new HashSet<>();
        for (SkippedDomain domain : skipped) {
            if (!seen.add(domain.domain())) {
                throw new IllegalArgumentException("Domain " + domain.domain().label() + " is skipped twice");
            }
        }
        skipped = skipped.stream().sorted(Comparator.comparingInt(domain -> domain.domain().rank())).toList();
        changes = changes.stream().sorted(ChangeOrder.COMPARATOR).toList();
    }

    public ChangeSet(VersionRef from, VersionRef to, List<Change> changes) {
        this(from, to, List.of(), changes);
    }

    public int schemaVersion() {
        return SCHEMA_VERSION;
    }
}
