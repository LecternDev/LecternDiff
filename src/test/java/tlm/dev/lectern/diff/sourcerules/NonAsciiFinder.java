package tlm.dev.lectern.diff.sourcerules;

import java.util.ArrayList;
import java.util.List;

final class NonAsciiFinder {

    private static final char MAX_ASCII = 0x7F;

    List<Violation> find(SourceFile file) {
        String text = file.text();
        LineIndex lines = new LineIndex(text);
        List<Violation> found = new ArrayList<>();
        for (int offset = 0; offset < text.length(); offset++) {
            char current = text.charAt(offset);
            if (current > MAX_ASCII) {
                String message = "non-ASCII character U+%04X".formatted((int) current);
                found.add(new Violation(file.path(), lines.lineOf(offset), message));
            }
        }
        return List.copyOf(found);
    }
}
