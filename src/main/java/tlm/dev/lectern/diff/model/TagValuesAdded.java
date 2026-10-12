package tlm.dev.lectern.diff.model;

import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public record TagValuesAdded(DataResource tag, List<String> values) implements TagChange {

    public TagValuesAdded {
        Objects.requireNonNull(tag, "tag");
        if (!tag.isTag()) {
            throw new IllegalArgumentException("Not a tag: " + tag);
        }
        TreeSet<String> distinct = new TreeSet<>(values);
        if (distinct.isEmpty() || distinct.size() != values.size()) {
            throw new IllegalArgumentException("The added values of " + tag + " are neither empty nor repeated");
        }
        values = List.copyOf(distinct);
    }
}
