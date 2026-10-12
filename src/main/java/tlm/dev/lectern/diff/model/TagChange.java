package tlm.dev.lectern.diff.model;

public sealed interface TagChange extends Change
        permits TagAdded, TagRemoved, TagValuesAdded, TagValuesRemoved, TagReplaceModified {
}
