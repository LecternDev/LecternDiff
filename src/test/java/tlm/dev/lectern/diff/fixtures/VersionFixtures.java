package tlm.dev.lectern.diff.fixtures;

import tlm.dev.lectern.diff.model.VersionId;
import tlm.dev.lectern.diff.model.VersionKind;
import tlm.dev.lectern.diff.model.VersionRef;

public final class VersionFixtures {

    private VersionFixtures() {
    }

    public static VersionRef release(String id) {
        return new VersionRef(new VersionId(id), new VersionKind.Release());
    }

    public static VersionRef snapshot(String id) {
        return new VersionRef(new VersionId(id), new VersionKind.Snapshot());
    }
}
