package tlm.dev.lectern.diff.model;

import java.util.List;
import java.util.Optional;

public sealed interface VersionKind
        permits VersionKind.Release, VersionKind.ReleaseCandidate, VersionKind.PreRelease, VersionKind.Snapshot {

    List<VersionKind> ALL = List.of(new Release(), new ReleaseCandidate(), new PreRelease(), new Snapshot());

    String label();

    static Optional<VersionKind> ofLabel(String label) {
        return ALL.stream().filter(kind -> kind.label().equals(label)).findFirst();
    }

    record Release() implements VersionKind {

        @Override
        public String label() {
            return "release";
        }
    }

    record ReleaseCandidate() implements VersionKind {

        @Override
        public String label() {
            return "release-candidate";
        }
    }

    record PreRelease() implements VersionKind {

        @Override
        public String label() {
            return "pre-release";
        }
    }

    record Snapshot() implements VersionKind {

        @Override
        public String label() {
            return "snapshot";
        }
    }
}
