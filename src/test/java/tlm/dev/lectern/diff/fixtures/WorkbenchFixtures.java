package tlm.dev.lectern.diff.fixtures;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import tlm.dev.lectern.diff.model.VersionId;
import tlm.dev.lectern.diff.model.VersionKind;
import tlm.dev.lectern.diff.model.VersionRef;
import tlm.dev.lectern.diff.read.DirectoryVersionTree;
import tlm.dev.lectern.diff.read.VersionTree;

public final class WorkbenchFixtures {

    private static final Path GENERATED = Path.of("..", "LecternWorkbench", "generated");
    private static final Map<String, VersionKind> KINDS = Map.of(
            "1.14.4", new VersionKind.Release(),
            "1.20.4", new VersionKind.Release(),
            "1.21.11", new VersionKind.Release(),
            "26.3", new VersionKind.Release(),
            "26.4-snapshot-3", new VersionKind.Snapshot());

    private WorkbenchFixtures() {
    }

    public static void requireReport(String file, String... versions) {
        for (String version : versions) {
            Path report = GENERATED.resolve(version).resolve(file);
            assumeTrue(Files.isRegularFile(report), "Workbench data missing: " + report.normalize()
                    + ". Download the server jar of " + version + " (SHA-1 checked against the Mojang manifest), run the"
                    + " official data generator with --server --reports into LecternWorkbench/generated/" + version
                    + "/ as described in LecternPipeline/CLAUDE.md, section Constats.");
        }
    }

    public static VersionTree tree(String version) {
        VersionKind kind = KINDS.get(version);
        if (kind == null) {
            throw new IllegalArgumentException("No kind known for the Workbench version " + version);
        }
        return new DirectoryVersionTree(GENERATED.resolve(version), new VersionRef(new VersionId(version), kind));
    }
}
