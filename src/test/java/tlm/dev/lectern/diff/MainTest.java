package tlm.dev.lectern.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tlm.dev.lectern.diff.fixtures.GadgetScenario;
import tlm.dev.lectern.diff.fixtures.VersionFolders;

class MainTest {

    @TempDir
    Path workDirectory;

    private record Outcome(int exitCode, String out, String err) {
    }

    private Path versionFolder(String name, String registriesJson, String blocksJson, String packetsJson, String commandsJson)
            throws IOException {
        return VersionFolders.write(workDirectory, name, registriesJson, blocksJson, packetsJson, commandsJson);
    }

    private Path before() throws IOException {
        return VersionFolders.before(workDirectory);
    }

    private Path after() throws IOException {
        return VersionFolders.after(workDirectory);
    }

    private static String[] diffArguments(Path from, Path to) {
        return new String[] {"diff", from.toString(), to.toString(),
                "--from", "1.0", "--from-kind", "release", "--to", "1.1", "--to-kind", "release"};
    }

    private static String[] without(String[] arguments, String option) {
        List<String> kept = new ArrayList<>(List.of(arguments));
        int index = kept.indexOf(option);
        kept.remove(index + 1);
        kept.remove(index);
        return kept.toArray(String[]::new);
    }

    private static Outcome run(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitCode = Main.create().run(args, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Outcome(exitCode, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    private static void assertUsageFailure(Outcome outcome, String detail) {
        assertEquals(Main.EXIT_USAGE, outcome.exitCode());
        assertEquals("error: " + detail + "\n" + CommandLine.USAGE + "\n", outcome.err());
        assertEquals("", outcome.out());
    }

    @Test
    void diffWritesTheChangeSetToTheStandardOutput() throws IOException {
        Outcome outcome = run(diffArguments(before(), after()));
        assertEquals(Main.EXIT_OK, outcome.exitCode());
        assertEquals(GadgetScenario.EXPECTED_JSON, outcome.out());
        assertEquals("", outcome.err());
    }

    @Test
    void theVersionComesFromTheOptionsNotFromTheFolderName() throws IOException {
        Path before = before();
        Path after = after();
        Outcome outcome = run("diff", before.toString(), after.toString(),
                "--from", "26.3", "--from-kind", "release", "--to", "26.4-snapshot-3", "--to-kind", "snapshot");
        assertEquals(Main.EXIT_OK, outcome.exitCode());
        assertTrue(outcome.out().contains("\"from\": {\n    \"id\": \"26.3\",\n    \"kind\": \"release\"\n  }"), outcome.out());
        assertTrue(outcome.out().contains("\"to\": {\n    \"id\": \"26.4-snapshot-3\",\n    \"kind\": \"snapshot\"\n  }"), outcome.out());
    }

    @Test
    void optionsAndFoldersMayComeInAnyOrder() throws IOException {
        Path before = before();
        Path after = after();
        Outcome outcome = run("diff", "--to-kind", "release", "--to", "1.1", before.toString(), "--from", "1.0",
                after.toString(), "--from-kind", "release");
        assertEquals(Main.EXIT_OK, outcome.exitCode());
        assertEquals(GadgetScenario.EXPECTED_JSON, outcome.out());
    }

    @Test
    void twoRunsWriteTheSameBytes() throws IOException {
        String[] arguments = diffArguments(before(), after());
        assertEquals(run(arguments).out(), run(arguments).out());
    }

    @Test
    void theOrderOfTheFoldersGivesTheDirectionOfTheDiff() throws IOException {
        Outcome reversed = run("diff", after().toString(), before().toString(),
                "--from", "1.1", "--from-kind", "release", "--to", "1.0", "--to-kind", "release");
        assertEquals(Main.EXIT_OK, reversed.exitCode());
        assertTrue(reversed.out().contains("\"from\": {\n    \"id\": \"1.1\""), reversed.out());
        assertTrue(reversed.out().contains("\"type\": \"registry.entry_removed\""), reversed.out());
        assertTrue(reversed.out().contains("\"type\": \"block.removed\""), reversed.out());
    }

    @Test
    void wrongArgumentsGiveTheErrorTheUsageAndExitCodeTwo() throws IOException {
        String[] valid = diffArguments(before(), after());
        assertUsageFailure(run(), "the first argument must be 'diff'");
        assertUsageFailure(run("compare", "a", "b"), "the first argument must be 'diff'");
        assertUsageFailure(run("diff"), "expected 2 version folders, got 0");
        assertUsageFailure(run("diff", "a", "--from", "1.0", "--from-kind", "release", "--to", "1.1", "--to-kind", "release"),
                "expected 2 version folders, got 1");
        List<String> threeFolders = new ArrayList<>(List.of(valid));
        threeFolders.add("extra");
        assertUsageFailure(run(threeFolders.toArray(String[]::new)), "expected 2 version folders, got 3");
        for (String option : List.of("--from", "--from-kind", "--to", "--to-kind")) {
            assertUsageFailure(run(without(valid, option)), "missing option " + option);
        }
    }

    @Test
    void aBadOptionGivesTheErrorTheUsageAndExitCodeTwo() throws IOException {
        Path before = before();
        Path after = after();
        assertUsageFailure(run("diff", before.toString(), after.toString(), "--bogus", "x"), "unknown option --bogus");
        assertUsageFailure(run("diff", before.toString(), after.toString(), "--from"), "option --from needs a value");
        assertUsageFailure(run("diff", before.toString(), after.toString(), "--from", "--to", "1.1"),
                "option --from needs a value");
        assertUsageFailure(run("diff", before.toString(), after.toString(), "--from", "1.0", "--from", "1.1"),
                "option --from is given twice");
    }

    @Test
    void anUnknownKindIsNamedInTheMessage() throws IOException {
        Path before = before();
        Path after = after();
        Outcome outcome = run("diff", before.toString(), after.toString(),
                "--from", "1.0", "--from-kind", "nightly", "--to", "1.1", "--to-kind", "release");
        assertUsageFailure(outcome,
                "unknown version kind 'nightly', expected one of release, release-candidate, pre-release, snapshot");
        Outcome other = run("diff", before.toString(), after.toString(),
                "--from", "1.0", "--from-kind", "release", "--to", "1.1", "--to-kind", "Release");
        assertUsageFailure(other,
                "unknown version kind 'Release', expected one of release, release-candidate, pre-release, snapshot");
    }

    @Test
    void aVersionIdThatCannotBeUsedIsNamedInTheMessage() throws IOException {
        Path before = before();
        Path after = after();
        Outcome outcome = run("diff", before.toString(), after.toString(),
                "--from", "a/b", "--from-kind", "release", "--to", "1.1", "--to-kind", "release");
        assertUsageFailure(outcome, "invalid version id 'a/b'");
    }

    @Test
    void aFolderThatDoesNotExistGivesExitCodeOne() throws IOException {
        Outcome outcome = run(diffArguments(workDirectory.resolve("missing"), after()));
        assertEquals(Main.EXIT_FAILURE, outcome.exitCode());
        assertTrue(outcome.err().startsWith("error: Not a version folder: "), outcome.err());
        assertEquals("", outcome.out());
    }

    @Test
    void aVersionWithoutAnyReportSkipsEveryDomainAndStillSucceeds() throws IOException {
        Path empty = Files.createDirectories(workDirectory.resolve("empty"));
        Outcome outcome = run(diffArguments(before(), empty));
        assertEquals(Main.EXIT_OK, outcome.exitCode());
        assertTrue(outcome.out().contains("""
                  "skipped": [
                    {
                      "domain": "registries",
                      "reason": "report_missing",
                      "side": "to"
                    },
                    {
                      "domain": "blocks",
                      "reason": "report_missing",
                      "side": "to"
                    },
                    {
                      "domain": "packets",
                      "reason": "report_missing",
                      "side": "to"
                    },
                    {
                      "domain": "commands",
                      "reason": "report_missing",
                      "side": "to"
                    },
                    {
                      "domain": "recipes",
                      "reason": "report_missing",
                      "side": "to"
                    },
                    {
                      "domain": "loot_tables",
                      "reason": "report_missing",
                      "side": "to"
                    },
                    {
                      "domain": "tags",
                      "reason": "report_missing",
                      "side": "to"
                    }
                  ],
                """), outcome.out());
        assertTrue(outcome.out().contains("\"changes\": [],"), outcome.out());
        assertEquals("", outcome.err());
    }

    @Test
    void aVersionWithoutItsBlocksReportSkipsOnlyTheBlocksDomain() throws IOException {
        Path reports = Files.createDirectories(workDirectory.resolve("partial").resolve("reports"));
        Files.writeString(reports.resolve("registries.json"), GadgetScenario.AFTER, StandardCharsets.UTF_8);
        Outcome outcome = run(diffArguments(before(), workDirectory.resolve("partial")));
        assertEquals(Main.EXIT_OK, outcome.exitCode());
        assertTrue(outcome.out().contains("\"domain\": \"blocks\""), outcome.out());
        assertTrue(outcome.out().contains("\"domain\": \"packets\""), outcome.out());
        assertTrue(outcome.out().contains("\"domain\": \"commands\""), outcome.out());
        assertTrue(outcome.out().contains("\"domain\": \"recipes\""), outcome.out());
        assertTrue(outcome.out().contains("\"side\": \"to\""), outcome.out());
        assertTrue(outcome.out().contains("\"type\": \"registry.added\""), outcome.out());
        assertTrue(!outcome.out().contains("\"type\": \"block."), outcome.out());
        assertTrue(!outcome.out().contains("\"type\": \"command."), outcome.out());
        assertEquals("", outcome.err());
    }

    @Test
    void anUnknownReportFormatGivesExitCodeOneAndNamesTheVersionAndTheFile() throws IOException {
        Path unknown = versionFolder("unknown",
                "{\"demo:gadget\": {\"entries\": {}, \"protocol_id\": 0, \"tags\": []}}", GadgetScenario.BLOCKS_AFTER,
                GadgetScenario.PACKETS_AFTER, GadgetScenario.COMMANDS_AFTER);
        Outcome outcome = run(diffArguments(before(), unknown));
        assertEquals(Main.EXIT_FAILURE, outcome.exitCode());
        assertEquals("error: Version 1.1, reports/registries.json: unknown key 'tags' in registry demo:gadget\n", outcome.err());
        assertEquals("", outcome.out());
    }

    @Test
    void anUnknownBlocksFormatGivesExitCodeOneAndNamesTheVersionAndTheFile() throws IOException {
        Path unknown = versionFolder("unknown", GadgetScenario.AFTER,
                "{\"demo:crate\": {\"states\": [{\"default\": true, \"id\": 0, \"weight\": 3}]}}", GadgetScenario.PACKETS_AFTER,
                GadgetScenario.COMMANDS_AFTER);
        Outcome outcome = run(diffArguments(before(), unknown));
        assertEquals(Main.EXIT_FAILURE, outcome.exitCode());
        assertEquals("error: Version 1.1, reports/blocks.json: unknown key 'weight' in a state of block demo:crate\n",
                outcome.err());
        assertEquals("", outcome.out());
    }

    @Test
    void anUnknownCommandsFormatGivesExitCodeOneAndNamesTheVersionAndTheFile() throws IOException {
        Path unknown = versionFolder("unknown", GadgetScenario.AFTER, GadgetScenario.BLOCKS_AFTER, GadgetScenario.PACKETS_AFTER,
                "{\"type\": \"root\", \"children\": {\"gadget\": {\"type\": \"literal\", \"hidden\": true}}}");
        Outcome outcome = run(diffArguments(before(), unknown));
        assertEquals(Main.EXIT_FAILURE, outcome.exitCode());
        assertEquals("error: Version 1.1, reports/commands.json: unknown key 'hidden' in node gadget\n", outcome.err());
        assertEquals("", outcome.out());
    }
}
