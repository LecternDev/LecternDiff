package tlm.dev.lectern.diff.read;

import java.util.List;
import java.util.regex.Pattern;
import tlm.dev.lectern.diff.model.VersionId;
import tlm.dev.lectern.diff.model.VersionKind;

public final class VersionKindReader {

    private static final String NUMBER = "(?:0|[1-9][0-9]{0,3})";
    private static final String COUNTER = "[1-9][0-9]{0,3}";
    private static final String OLD_RELEASE = "1\\." + NUMBER + "(?:\\." + NUMBER + ")?";
    private static final String NEW_RELEASE = "[0-9]{2}\\." + NUMBER + "(?:\\." + NUMBER + ")?";

    private record Form(Pattern pattern, VersionKind kind) {

        static Form of(String regex, VersionKind kind) {
            return new Form(Pattern.compile(regex), kind);
        }
    }

    private static final List<Form> FORMS = List.of(
            Form.of(OLD_RELEASE, new VersionKind.Release()),
            Form.of(OLD_RELEASE + "-rc" + COUNTER, new VersionKind.ReleaseCandidate()),
            Form.of(OLD_RELEASE + "-pre" + COUNTER, new VersionKind.PreRelease()),
            Form.of("[0-9]{2}w[0-9]{2}[a-z]", new VersionKind.Snapshot()),
            Form.of(NEW_RELEASE, new VersionKind.Release()),
            Form.of(NEW_RELEASE + "-rc-" + COUNTER, new VersionKind.ReleaseCandidate()),
            Form.of(NEW_RELEASE + "-pre-" + COUNTER, new VersionKind.PreRelease()),
            Form.of(NEW_RELEASE + "-snapshot-" + COUNTER, new VersionKind.Snapshot()));

    public VersionKind read(VersionId id) {
        for (Form form : FORMS) {
            if (form.pattern().matcher(id.value()).matches()) {
                return form.kind();
            }
        }
        throw new VersionReadException("Cannot tell the kind of version '" + id.value()
                + "': its id is not shaped like a release, a release candidate, a pre-release or a snapshot");
    }
}
