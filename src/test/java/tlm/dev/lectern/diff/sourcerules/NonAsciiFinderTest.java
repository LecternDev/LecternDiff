package tlm.dev.lectern.diff.sourcerules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class NonAsciiFinderTest {

    private final NonAsciiFinder finder = new NonAsciiFinder();

    @Test
    void acceptsAsciiSourceIncludingAnEscapeSequence() {
        String escape = "\\" + "u00e9";
        String source = "class A { String s = \"" + escape + "\"; }";
        assertEquals(List.of(), finder.find(new SourceFile("A.java", source)));
    }

    @Test
    void reportsNonAsciiCharacterWithItsLine() {
        String accented = String.valueOf((char) 0xE9);
        String source = "class A {\n    String s = \"" + accented + "\";\n}\n";
        assertEquals(List.of(new Violation("A.java", 2, "non-ASCII character U+00E9")),
                finder.find(new SourceFile("A.java", source)));
    }
}
