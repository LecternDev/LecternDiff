package tlm.dev.lectern.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tlm.dev.lectern.diff.fixtures.GadgetScenario;

class MainTest {

    private static final String USAGE_LINE = "usage: diff <version-folder-A> <version-folder-B>\n";

    @TempDir
    Path workDirectory;

    private record Outcome(int exitCode, String out, String err) {
    }

    private Path versionFolder(String id, String registriesJson) throws IOException {
        Path reports = Files.createDirectories(workDirectory.resolve(id).resolve("reports"));
        Files.writeString(reports.resolve("registries.json"), registriesJson, StandardCharsets.UTF_8);
        return workDirectory.resolve(id);
    }

    private static Outcome run(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitCode = Main.create().run(args, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Outcome(exitCode, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void diffWritesTheChangeSetToTheStandardOutput() throws IOException {
        Path before = versionFolder("1.0", GadgetScenario.BEFORE);
        Path after = versionFolder("1.1", GadgetScenario.AFTER);
        Outcome outcome = run("diff", before.toString(), after.toString());
        assertEquals(Main.EXIT_OK, outcome.exitCode());
        assertEquals(GadgetScenario.EXPECTED_JSON, outcome.out());
        assertEquals("", outcome.err());
    }

    @Test
    void twoRunsWriteTheSameBytes() throws IOException {
        Path before = versionFolder("1.0", GadgetScenario.BEFORE);
        Path after = versionFolder("1.1", GadgetScenario.AFTER);
        assertEquals(run("diff", before.toString(), after.toString()).out(), run("diff", before.toString(), after.toString()).out());
    }

    @Test
    void theOrderOfTheArgumentsGivesTheDirectionOfTheDiff() throws IOException {
        Path before = versionFolder("1.0", GadgetScenario.BEFORE);
        Path after = versionFolder("1.1", GadgetScenario.AFTER);
        Outcome reversed = run("diff", after.toString(), before.toString());
        assertEquals(Main.EXIT_OK, reversed.exitCode());
        assertTrue(reversed.out().contains("\"from\": {\n    \"id\": \"1.1\""), reversed.out());
        assertTrue(reversed.out().contains("\"type\": \"registry.entry_removed\""), reversed.out());
    }

    @Test
    void wrongArgumentsGiveTheUsageAndExitCodeTwo() throws IOException {
        Path folder = versionFolder("1.0", GadgetScenario.BEFORE);
        String path = folder.toString();
        for (String[] args : new String[][] {{}, {"diff"}, {"diff", path}, {"compare", path, path}, {"diff", path, path, path}}) {
            Outcome outcome = run(args);
            assertEquals(Main.EXIT_USAGE, outcome.exitCode(), String.join(" ", args));
            assertEquals(USAGE_LINE, outcome.err());
            assertEquals("", outcome.out());
        }
    }

    @Test
    void aFolderThatDoesNotExistGivesExitCodeOne() throws IOException {
        Path after = versionFolder("1.1", GadgetScenario.AFTER);
        Outcome outcome = run("diff", workDirectory.resolve("1.0").toString(), after.toString());
        assertEquals(Main.EXIT_FAILURE, outcome.exitCode());
        assertTrue(outcome.err().startsWith("error: Not a version folder: "), outcome.err());
        assertEquals("", outcome.out());
    }

    @Test
    void aVersionWithoutItsReportGivesExitCodeOneAndNamesTheVersionAndTheFile() throws IOException {
        Path before = versionFolder("1.0", GadgetScenario.BEFORE);
        Path empty = Files.createDirectories(workDirectory.resolve("1.1"));
        Outcome outcome = run("diff", before.toString(), empty.toString());
        assertEquals(Main.EXIT_FAILURE, outcome.exitCode());
        assertEquals("error: Version 1.1, reports/registries.json: the report is missing\n", outcome.err());
        assertEquals("", outcome.out());
    }

    @Test
    void anUnknownReportFormatGivesExitCodeOneAndNamesTheVersionAndTheFile() throws IOException {
        Path before = versionFolder("1.0", GadgetScenario.BEFORE);
        Path unknown = versionFolder("1.1", "{\"demo:gadget\": {\"entries\": {}, \"protocol_id\": 0, \"tags\": []}}");
        Outcome outcome = run("diff", before.toString(), unknown.toString());
        assertEquals(Main.EXIT_FAILURE, outcome.exitCode());
        assertEquals("error: Version 1.1, reports/registries.json: unknown key 'tags' in registry demo:gadget\n", outcome.err());
        assertEquals("", outcome.out());
    }

    @Test
    void aFolderWhoseNameIsNotAVersionGivesExitCodeOne() throws IOException {
        Path before = versionFolder("1.0", GadgetScenario.BEFORE);
        Path other = versionFolder("backup", GadgetScenario.AFTER);
        Outcome outcome = run("diff", before.toString(), other.toString());
        assertEquals(Main.EXIT_FAILURE, outcome.exitCode());
        assertTrue(outcome.err().startsWith("error: Cannot tell the kind of version 'backup'"), outcome.err());
    }
}
