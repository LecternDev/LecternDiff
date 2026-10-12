package tlm.dev.lectern.diff.model;

public sealed interface VersionKind
        permits VersionKind.Release, VersionKind.ReleaseCandidate, VersionKind.PreRelease, VersionKind.Snapshot {

    String label();

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
