package tlm.dev.lectern.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tlm.dev.lectern.diff.fixtures.WorkbenchFixtures.generatedFolder;
import static tlm.dev.lectern.diff.fixtures.WorkbenchFixtures.kindLabel;
import static tlm.dev.lectern.diff.fixtures.WorkbenchFixtures.normalizedFolder;
import static tlm.dev.lectern.diff.fixtures.WorkbenchFixtures.requireNormalized;
import static tlm.dev.lectern.diff.fixtures.WorkbenchFixtures.requireReport;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.read.RegistryReportReader;

class WorkbenchNormalizedDiffTest {

    private static final List<List<String>> PAIRS = List.of(
            List.of("26.3", "26.4-snapshot-3"),
            List.of("26.3-rc-3", "26.3"),
            List.of("1.21.11", "26.3"),
            List.of("1.20.4", "1.21.11"),
            List.of("1.14.4", "1.20.4"));

    private static String run(Path from, Path to, String fromId, String toId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitCode = Main.create().run(
                new String[] {"diff", from.toString(), to.toString(),
                        "--from", fromId, "--from-kind", kindLabel(fromId), "--to", toId, "--to-kind", kindLabel(toId)},
                new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(err, true, StandardCharsets.UTF_8));
        assertEquals(0, exitCode, err.toString(StandardCharsets.UTF_8));
        assertEquals("", err.toString(StandardCharsets.UTF_8));
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void normalizedFoldersGiveTheSameBytesAsGeneratedFoldersForEveryObservedPair() {
        for (List<String> pair : PAIRS) {
            String from = pair.get(0);
            String to = pair.get(1);
            requireReport(RegistryReportReader.FILE, from, to);
            requireNormalized(from, to);
            String generated = run(generatedFolder(from), generatedFolder(to), from, to);
            String normalized = run(normalizedFolder(from), normalizedFolder(to), from, to);
            assertTrue(generated.startsWith("{\n"), from + " to " + to);
            assertEquals(generated, normalized, from + " to " + to);
        }
    }
}
