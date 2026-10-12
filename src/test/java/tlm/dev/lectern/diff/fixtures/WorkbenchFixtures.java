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
    private static final Path NORMALIZED = Path.of("..", "LecternWorkbench", "normalized");
    private static final Map<String, VersionKind> KINDS = Map.of(
            "1.14.4", new VersionKind.Release(),
            "1.20.4", new VersionKind.Release(),
            "1.21.11", new VersionKind.Release(),
            "26.3", new VersionKind.Release(),
            "26.3-rc-3", new VersionKind.ReleaseCandidate(),
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

    public static void requireData(String... versions) {
        for (String version : versions) {
            Path data = GENERATED.resolve(version).resolve("data");
            assumeTrue(Files.isDirectory(data), "Workbench data missing: " + data.normalize()
                    + ". Download the server jar of " + version + " (SHA-1 checked against the Mojang manifest), run the"
                    + " official data generator with --server --reports into LecternWorkbench/generated/" + version
                    + "/ as described in LecternPipeline/CLAUDE.md, section Constats.");
        }
    }

    public static Path generatedFolder(String version) {
        return GENERATED.resolve(version);
    }

    public static Path normalizedFolder(String version) {
        return NORMALIZED.resolve(version);
    }

    public static void requireNormalized(String... versions) {
        for (String version : versions) {
            Path reports = NORMALIZED.resolve(version).resolve("reports");
            assumeTrue(Files.isDirectory(reports), "Workbench data missing: " + reports.normalize()
                    + ". Run LecternPipeline on " + version + " to write LecternWorkbench/normalized/" + version
                    + "/ as described in LecternPipeline/CLAUDE.md.");
        }
    }

    public static String kindLabel(String version) {
        VersionKind kind = KINDS.get(version);
        if (kind == null) {
            throw new IllegalArgumentException("No kind known for the Workbench version " + version);
        }
        return kind.label();
    }

    public static VersionTree tree(String version) {
        VersionKind kind = KINDS.get(version);
        if (kind == null) {
            throw new IllegalArgumentException("No kind known for the Workbench version " + version);
        }
        return new DirectoryVersionTree(GENERATED.resolve(version), new VersionRef(new VersionId(version), kind));
    }
}
