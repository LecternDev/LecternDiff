package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record TagRemoved(DataResource tag, int valueCount) implements TagChange {

    public TagRemoved {
        Objects.requireNonNull(tag, "tag");
        if (!tag.isTag()) {
            throw new IllegalArgumentException("Not a tag: " + tag);
        }
        if (valueCount < 0) {
            throw new IllegalArgumentException("A value count is not negative: " + valueCount);
        }
    }
}
