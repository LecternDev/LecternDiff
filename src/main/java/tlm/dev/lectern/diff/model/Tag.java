package tlm.dev.lectern.diff.model;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public record Tag(boolean replace, Set<String> values) {

    public Tag {
        for (String value : values) {
            if (value.isEmpty()) {
                throw new IllegalArgumentException("A tag value is not empty");
            }
        }
        values = Collections.unmodifiableSet(new TreeSet<>(values));
    }
}
