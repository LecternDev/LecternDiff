package tlm.dev.lectern.diff.model;

import java.util.Comparator;

final class ChangeOrder {

    static final Comparator<Change> COMPARATOR = Comparator
            .comparingInt((Change change) -> keyOf(change).group())
            .thenComparing(change -> keyOf(change).subject())
            .thenComparingInt(change -> keyOf(change).rank())
            .thenComparing(change -> keyOf(change).detail());

    private static final int REGISTRIES = 0;

    private record Key(int group, String subject, int rank, String detail) {
    }

    private ChangeOrder() {
    }

    private static Key keyOf(Change change) {
        return switch (change) {
            case RegistryIdsShifted _ -> new Key(REGISTRIES, "", 0, "");
            case RegistryAdded added -> new Key(REGISTRIES, added.registry().value(), 1, "");
            case RegistryRemoved removed -> new Key(REGISTRIES, removed.registry().value(), 2, "");
            case RegistryDefaultModified modified -> new Key(REGISTRIES, modified.registry().value(), 3, "");
            case RegistryIdModified modified -> new Key(REGISTRIES, modified.registry().value(), 4, "");
            case RegistryEntryAdded added -> new Key(REGISTRIES, added.registry().value(), 5, added.entry().value());
            case RegistryEntryRemoved removed ->
                    new Key(REGISTRIES, removed.registry().value(), 6, removed.entry().value());
            case RegistryEntryIdModified modified ->
                    new Key(REGISTRIES, modified.registry().value(), 7, modified.entry().value());
            case RegistryEntryIdsShifted shifted -> new Key(REGISTRIES, shifted.registry().value(), 8, "");
        };
    }
}
