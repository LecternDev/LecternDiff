package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record TagReplaceModified(DataResource tag, boolean oldReplace, boolean newReplace) implements TagChange {

    public TagReplaceModified {
        Objects.requireNonNull(tag, "tag");
        if (!tag.isTag()) {
            throw new IllegalArgumentException("Not a tag: " + tag);
        }
        if (oldReplace == newReplace) {
            throw new IllegalArgumentException("The replace flag of " + tag + " did not change");
        }
    }
}
