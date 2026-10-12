package tlm.dev.lectern.diff.model;

import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public record TagValuesRemoved(DataResource tag, List<String> values) implements TagChange {

    public TagValuesRemoved {
        Objects.requireNonNull(tag, "tag");
        if (!tag.isTag()) {
            throw new IllegalArgumentException("Not a tag: " + tag);
        }
        TreeSet<String> distinct = new TreeSet<>(values);
        if (distinct.isEmpty() || distinct.size() != values.size()) {
            throw new IllegalArgumentException("The removed values of " + tag + " are neither empty nor repeated");
        }
        values = List.copyOf(distinct);
    }
}
