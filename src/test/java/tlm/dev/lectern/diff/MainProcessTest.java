package tlm.dev.lectern.diff;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tlm.dev.lectern.diff.fixtures.GadgetScenario;
import tlm.dev.lectern.diff.fixtures.VersionFolders;

class MainProcessTest {

    private static final String CLASSPATH_PROPERTY = "lectern.diff.runtimeClasspath";

    @TempDir
    Path workDirectory;

    private record Outcome(int exitCode, byte[] out, byte[] err) {

        String errText() {
            return new String(err, StandardCharsets.UTF_8);
        }
    }

    private Outcome run(String... args) throws IOException, InterruptedException {
        Path out = workDirectory.resolve("process-stdout.bin");
        Path err = workDirectory.resolve("process-stderr.bin");
        List<String> command = new ArrayList<>();
        command.add(ProcessHandle.current().info().command().orElseThrow());
        command.add("-cp");
        command.add(System.getProperty(CLASSPATH_PROPERTY));
        command.add("tlm.dev.lectern.diff.Main");
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command)
                .redirectOutput(out.toFile())
                .redirectError(err.toFile())
                .start();
        process.getOutputStream().close();
        assertTrue(process.waitFor(120, TimeUnit.SECONDS), "the process did not end");
        return new Outcome(process.exitValue(), Files.readAllBytes(out), Files.readAllBytes(err));
    }

    private static String[] diffArguments(Path from, Path to) {
        return new String[] {"diff", from.toString(), to.toString(),
                "--from", "1.0", "--from-kind", "release", "--to", "1.1", "--to-kind", "release"};
    }

    @Test
    void aSuccessfulRunExitsWithZeroAndWritesOnlyTheCanonicalJsonToTheStandardOutput() throws Exception {
        Outcome outcome = run(diffArguments(VersionFolders.before(workDirectory), VersionFolders.after(workDirectory)));
        assertEquals(0, outcome.exitCode());
        assertArrayEquals(GadgetScenario.EXPECTED_JSON.getBytes(StandardCharsets.UTF_8), outcome.out());
        assertEquals(0, outcome.err().length, outcome.errText());
    }

    @Test
    void twoProcessesWriteTheSameBytes() throws Exception {
        String[] arguments = diffArguments(VersionFolders.before(workDirectory), VersionFolders.after(workDirectory));
        assertArrayEquals(run(arguments).out(), run(arguments).out());
    }

    @Test
    void wrongArgumentsExitWithTwoAndLeaveTheStandardOutputEmpty() throws Exception {
        Outcome noArguments = run();
        assertEquals(2, noArguments.exitCode());
        assertEquals(0, noArguments.out().length);
        assertEquals("error: the first argument must be 'diff'\n" + CommandLine.USAGE + "\n", noArguments.errText());
        Outcome oneFolder = run("diff", "only-one", "--from", "1.0", "--from-kind", "release", "--to", "1.1", "--to-kind", "release");
        assertEquals(2, oneFolder.exitCode());
        assertEquals(0, oneFolder.out().length);
        assertEquals("error: expected 2 version folders, got 1\n" + CommandLine.USAGE + "\n", oneFolder.errText());
    }

    @Test
    void anUnknownKindExitsWithTwoAndNamesIt() throws Exception {
        Outcome outcome = run("diff", VersionFolders.before(workDirectory).toString(), VersionFolders.after(workDirectory).toString(),
                "--from", "1.0", "--from-kind", "nightly", "--to", "1.1", "--to-kind", "release");
        assertEquals(2, outcome.exitCode());
        assertEquals(0, outcome.out().length);
        assertTrue(outcome.errText().startsWith("error: unknown version kind 'nightly'"), outcome.errText());
    }

    @Test
    void aFolderThatDoesNotExistExitsWithOneAndLeavesTheStandardOutputEmpty() throws Exception {
        Outcome outcome = run(diffArguments(workDirectory.resolve("missing"), VersionFolders.after(workDirectory)));
        assertEquals(1, outcome.exitCode());
        assertEquals(0, outcome.out().length);
        assertTrue(outcome.errText().startsWith("error: Not a version folder: "), outcome.errText());
        assertEquals(1, outcome.errText().lines().count(), outcome.errText());
    }

    @Test
    void anUnknownReportFormatExitsWithOneAndNamesTheVersionAndTheFile() throws Exception {
        Path unknown = VersionFolders.write(workDirectory, "unknown", GadgetScenario.AFTER, GadgetScenario.BLOCKS_AFTER,
                GadgetScenario.PACKETS_AFTER, "{\"type\": \"root\", \"children\": {\"gadget\": {\"type\": \"literal\", \"hidden\": true}}}");
        Outcome outcome = run(diffArguments(VersionFolders.before(workDirectory), unknown));
        assertEquals(1, outcome.exitCode());
        assertEquals(0, outcome.out().length);
        assertEquals("error: Version 1.1, reports/commands.json: unknown key 'hidden' in node gadget\n", outcome.errText());
    }

    @Test
    void aMissingReportIsNotAFailureAndIsDeclaredInTheOutput() throws Exception {
        Path partial = Files.createDirectories(workDirectory.resolve("partial").resolve("reports"));
        Files.writeString(partial.resolve("registries.json"), GadgetScenario.AFTER, StandardCharsets.UTF_8);
        Outcome outcome = run(diffArguments(VersionFolders.before(workDirectory), workDirectory.resolve("partial")));
        assertEquals(0, outcome.exitCode());
        assertEquals(0, outcome.err().length, outcome.errText());
        String text = new String(outcome.out(), StandardCharsets.UTF_8);
        assertTrue(text.contains("\"domain\": \"commands\""), text);
        assertTrue(text.endsWith("}\n"), text);
    }
}
