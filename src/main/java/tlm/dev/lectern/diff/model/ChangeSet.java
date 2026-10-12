package tlm.dev.lectern.diff.model;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ChangeSet(VersionRef from, VersionRef to, List<Skipped> skipped, List<Change> changes) {

    public static final int SCHEMA_VERSION = 1;

    private static final Comparator<Skipped> SKIPPED_ORDER = Comparator
            .comparingInt((Skipped skipped) -> switch (skipped) {
                case SkippedDomain domain -> domain.domain().rank();
                case SkippedFolder _ -> Integer.MAX_VALUE;
            })
            .thenComparing(skipped -> switch (skipped) {
                case SkippedDomain _ -> "";
                case SkippedFolder folder -> folder.folder();
            });

    public ChangeSet {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Set<String> seen = new HashSet<>();
        for (Skipped entry : skipped) {
            String name = switch (entry) {
                case SkippedDomain domain -> "domain " + domain.domain().label();
                case SkippedFolder folder -> "folder " + folder.folder();
            };
            if (!seen.add(name)) {
                throw new IllegalArgumentException("The " + name + " is skipped twice");
            }
        }
        skipped = skipped.stream().sorted(SKIPPED_ORDER).toList();
        changes = changes.stream().sorted(ChangeOrder.COMPARATOR).toList();
    }

    public ChangeSet(VersionRef from, VersionRef to, List<Change> changes) {
        this(from, to, List.of(), changes);
    }

    public int schemaVersion() {
        return SCHEMA_VERSION;
    }
}
