package tlm.dev.lectern.diff.sourcerules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class FamilyViolationFinderTest {

    private static final List<SeamRule> SEAMS = List.of(
            new SeamRule("model", List.of("blocks", "recipes")),
            new SeamRule("blocks", List.of("recipes", "read")));

    private final FamilyViolationFinder finder = new FamilyViolationFinder();

    @Test
    void acceptsAllowedReferencesRootClassesAndFamiliesWithoutSeam() {
        List<SourceFile> files = List.of(
                new SourceFile("tlm/dev/lectern/diff/model/Change.java", """
                        package tlm.dev.lectern.diff.model;

                        record Change() {
                        }
                        """),
                new SourceFile("tlm/dev/lectern/diff/blocks/BlockChange.java", """
                        package tlm.dev.lectern.diff.blocks;

                        import tlm.dev.lectern.diff.DiffService;
                        import tlm.dev.lectern.diff.model.Change;
                        """),
                new SourceFile("tlm/dev/lectern/diff/read/Reader.java", """
                        package tlm.dev.lectern.diff.read;

                        import tlm.dev.lectern.diff.blocks.BlockChange;
                        """),
                new SourceFile("tlm/dev/lectern/diff/DiffService.java", """
                        package tlm.dev.lectern.diff;

                        import tlm.dev.lectern.diff.blocks.BlockChange;
                        import tlm.dev.lectern.diff.recipes.RecipeChange;
                        """));
        assertEquals(List.of(), finder.forbiddenReferences(SEAMS, files));
    }

    @Test
    void reportsImportOfAForbiddenFamilyWithItsLine() {
        List<SourceFile> files = List.of(
                new SourceFile("tlm/dev/lectern/diff/model/Change.java", """
                        package tlm.dev.lectern.diff.model;

                        import tlm.dev.lectern.diff.blocks.BlockChange;
                        """));
        assertEquals(
                List.of(new Violation("tlm/dev/lectern/diff/model/Change.java", 3,
                        "family model references blocks (seam model -> blocks)")),
                finder.forbiddenReferences(SEAMS, files));
    }

    @Test
    void reportsQualifiedNamesWildcardsAndStaticImports() {
        List<SourceFile> files = List.of(
                new SourceFile("tlm/dev/lectern/diff/blocks/BlockChange.java", """
                        package tlm.dev.lectern.diff.blocks;

                        import tlm.dev.lectern.diff.read.*;
                        import static tlm.dev.lectern.diff.recipes.RecipeChange.of;

                        final class BlockChange {
                            tlm.dev.lectern.diff.recipes.RecipeChange other;
                        }
                        """));
        assertEquals(
                List.of(
                        new Violation("tlm/dev/lectern/diff/blocks/BlockChange.java", 3,
                                "family blocks references read (seam blocks -> read)"),
                        new Violation("tlm/dev/lectern/diff/blocks/BlockChange.java", 4,
                                "family blocks references recipes (seam blocks -> recipes)"),
                        new Violation("tlm/dev/lectern/diff/blocks/BlockChange.java", 7,
                                "family blocks references recipes (seam blocks -> recipes)")),
                finder.forbiddenReferences(SEAMS, files));
    }

    @Test
    void reportsEachPackageMissingFromTheDeclaredFamiliesOnce() {
        List<SourceFile> files = List.of(
                new SourceFile("tlm/dev/lectern/diff/extras/A.java", "package tlm.dev.lectern.diff.extras;\n"),
                new SourceFile("tlm/dev/lectern/diff/extras/B.java", "package tlm.dev.lectern.diff.extras;\n"),
                new SourceFile("tlm/dev/lectern/diff/model/C.java", "package tlm.dev.lectern.diff.model;\n"),
                new SourceFile("tlm/dev/lectern/diff/DiffService.java", "package tlm.dev.lectern.diff;\n"));
        assertEquals(
                List.of(new Violation("tlm/dev/lectern/diff/extras/A.java", 1,
                        "package extras is not a declared family")),
                finder.undeclaredFamilies(List.of("model"), files));
    }
}
