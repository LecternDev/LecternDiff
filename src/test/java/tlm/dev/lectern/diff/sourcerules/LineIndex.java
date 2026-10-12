package tlm.dev.lectern.diff.sourcerules;

import java.util.Arrays;

final class LineIndex {

    private final int[] lineStarts;

    LineIndex(String text) {
        int[] starts = new int[text.length() + 1];
        int count = 0;
        starts[count++] = 0;
        for (int offset = 0; offset < text.length(); offset++) {
            if (text.charAt(offset) == '\n') {
                starts[count++] = offset + 1;
            }
        }
        this.lineStarts = Arrays.copyOf(starts, count);
    }

    int lineOf(int offset) {
        int found = Arrays.binarySearch(lineStarts, offset);
        return found >= 0 ? found + 1 : -found - 1;
    }
}
