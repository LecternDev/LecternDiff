package tlm.dev.lectern.diff.sourcerules;

import java.util.ArrayList;
import java.util.List;

final class CommentFinder {

    private static final String TEXT_BLOCK_DELIMITER = "\"\"\"";

    List<Violation> find(SourceFile file) {
        String text = file.text();
        LineIndex lines = new LineIndex(text);
        List<Violation> found = new ArrayList<>();
        int offset = 0;
        while (offset < text.length()) {
            if (text.startsWith("//", offset)) {
                found.add(new Violation(file.path(), lines.lineOf(offset), "line comment"));
                offset = endOfLineComment(text, offset);
            } else if (text.startsWith("/*", offset)) {
                found.add(new Violation(file.path(), lines.lineOf(offset), "block comment"));
                offset = endOfBlockComment(text, offset);
            } else if (text.startsWith(TEXT_BLOCK_DELIMITER, offset)) {
                offset = endOfTextBlock(text, offset);
            } else if (text.charAt(offset) == '"' || text.charAt(offset) == '\'') {
                offset = endOfQuotedLiteral(text, offset);
            } else {
                offset++;
            }
        }
        return List.copyOf(found);
    }

    private static int endOfLineComment(String text, int start) {
        int end = text.indexOf('\n', start);
        return end < 0 ? text.length() : end;
    }

    private static int endOfBlockComment(String text, int start) {
        int end = text.indexOf("*/", start + 2);
        return end < 0 ? text.length() : end + 2;
    }

    private static int endOfTextBlock(String text, int start) {
        int offset = start + TEXT_BLOCK_DELIMITER.length();
        while (offset < text.length()) {
            if (text.charAt(offset) == '\\') {
                offset += 2;
            } else if (text.startsWith(TEXT_BLOCK_DELIMITER, offset)) {
                return offset + TEXT_BLOCK_DELIMITER.length();
            } else {
                offset++;
            }
        }
        return text.length();
    }

    private static int endOfQuotedLiteral(String text, int start) {
        char quote = text.charAt(start);
        int offset = start + 1;
        while (offset < text.length()) {
            char current = text.charAt(offset);
            if (current == '\\') {
                offset += 2;
            } else if (current == quote) {
                return offset + 1;
            } else if (current == '\n') {
                return offset;
            } else {
                offset++;
            }
        }
        return text.length();
    }
}
