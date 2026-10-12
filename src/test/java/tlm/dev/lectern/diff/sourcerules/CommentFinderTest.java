package tlm.dev.lectern.diff.sourcerules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CommentFinderTest {

    private final CommentFinder finder = new CommentFinder();

    @Test
    void acceptsSlashesAndStarsInsideLiterals() {
        String source = """
                class A {
                    String url = "https://example.test/a";
                    String star = "/* not a comment */";
                    String escaped = "quote \\" then // still text";
                    char quote = '"';
                    char slash = '/';
                    String block = \"\"\"
                        // text block line
                        /* text block star */
                        \"\"\";
                }
                """;
        assertEquals(List.of(), finder.find(new SourceFile("A.java", source)));
    }

    @Test
    void reportsLineCommentWithItsLine() {
        String source = """
                class A {
                    int a; // trailing
                }
                """;
        assertEquals(List.of(new Violation("A.java", 2, "line comment")),
                finder.find(new SourceFile("A.java", source)));
    }

    @Test
    void reportsBlockCommentOnItsFirstLine() {
        String source = """
                class A {
                    /**
                     * Documented.
                     */
                    int a;
                }
                """;
        assertEquals(List.of(new Violation("A.java", 2, "block comment")),
                finder.find(new SourceFile("A.java", source)));
    }

    @Test
    void keepsFindingCommentsAfterEveryKindOfLiteral() {
        String source = """
                class A {
                    String text = "a\\"b";
                    char quote = '"';
                    String block = \"\"\"
                        inside
                        \"\"\";
                    int a; // after literals
                }
                """;
        assertEquals(List.of(new Violation("A.java", 7, "line comment")),
                finder.find(new SourceFile("A.java", source)));
    }
}
