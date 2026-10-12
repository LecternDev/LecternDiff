package tlm.dev.lectern.diff.sourcerules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class SourceRulesTest {

    private static final String MAIN_SOURCES = "src/main/java";
    private static final String TEST_SOURCES = "src/test/java";
    private static final String RULES_FILE = "ARCHITECTURE_RULES.md";

    private final Path projectDirectory = Path.of("").toAbsolutePath();
    private final SourceTree sources = new SourceTree(projectDirectory);

    @Test
    void mainSourcesContainNoComment() {
        assertNoViolations(MAIN_SOURCES, findComments(sources.readJavaSources(MAIN_SOURCES)));
    }

    @Test
    void testSourcesContainNoComment() {
        assertNoViolations(TEST_SOURCES, findComments(sources.readJavaSources(TEST_SOURCES)));
    }

    @Test
    void mainSourcesContainOnlyAscii() {
        NonAsciiFinder finder = new NonAsciiFinder();
        List<Violation> violations = sources.readJavaSources(MAIN_SOURCES).stream()
                .flatMap(file -> finder.find(file).stream())
                .toList();
        assertNoViolations(MAIN_SOURCES, violations);
    }

    @Test
    void everyMainPackageIsADeclaredFamily() throws IOException {
        ArchitectureRules rules = readRules();
        assertFalse(rules.families().isEmpty(), RULES_FILE + " declares no family");
        List<Violation> violations = new FamilyViolationFinder()
                .undeclaredFamilies(rules.families(), sources.readJavaSources(MAIN_SOURCES));
        assertNoViolations(MAIN_SOURCES, violations);
    }

    @Test
    void declaredSeamsStayEmpty() throws IOException {
        ArchitectureRules rules = readRules();
        assertFalse(rules.seams().isEmpty(), RULES_FILE + " declares no seam");
        assertEquals(List.of(), rules.undeclaredSeamFamilies(),
                "seam families missing from the family table of " + RULES_FILE);
        List<Violation> violations = new FamilyViolationFinder()
                .forbiddenReferences(rules.seams(), sources.readJavaSources(MAIN_SOURCES));
        assertNoViolations(MAIN_SOURCES, violations);
    }

    private ArchitectureRules readRules() throws IOException {
        Path rulesFile = projectDirectory.resolve(RULES_FILE);
        assumeTrue(Files.isRegularFile(rulesFile),
                RULES_FILE + " is never committed, family and seam checks are skipped on a clone without it");
        return ArchitectureRules.parse(Files.readString(rulesFile, StandardCharsets.UTF_8));
    }

    private static List<Violation> findComments(List<SourceFile> files) {
        CommentFinder finder = new CommentFinder();
        return files.stream().flatMap(file -> finder.find(file).stream()).toList();
    }

    private static void assertNoViolations(String directory, List<Violation> violations) {
        List<String> described = violations.stream().map(Violation::describe).toList();
        assertEquals(List.of(), described, "violations in " + directory);
    }
}
