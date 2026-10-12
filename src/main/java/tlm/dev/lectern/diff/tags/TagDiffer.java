package tlm.dev.lectern.diff.tags;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import tlm.dev.lectern.diff.model.DataFiles;
import tlm.dev.lectern.diff.model.DataResource;
import tlm.dev.lectern.diff.model.Tag;
import tlm.dev.lectern.diff.model.TagAdded;
import tlm.dev.lectern.diff.model.TagChange;
import tlm.dev.lectern.diff.model.TagRemoved;
import tlm.dev.lectern.diff.model.TagReplaceModified;
import tlm.dev.lectern.diff.model.TagValuesAdded;
import tlm.dev.lectern.diff.model.TagValuesRemoved;

public final class TagDiffer {

    public List<TagChange> diff(DataFiles<Tag> from, DataFiles<Tag> to) {
        List<TagChange> changes = new ArrayList<>();
        for (Map.Entry<DataResource, Tag> entry : from.files().entrySet()) {
            Tag after = to.files().get(entry.getKey());
            if (after == null) {
                changes.add(new TagRemoved(entry.getKey(), entry.getValue().values().size()));
            } else {
                compare(entry.getKey(), entry.getValue(), after, changes);
            }
        }
        for (Map.Entry<DataResource, Tag> entry : to.files().entrySet()) {
            if (!from.files().containsKey(entry.getKey())) {
                changes.add(new TagAdded(entry.getKey(), entry.getValue().values().size()));
            }
        }
        return List.copyOf(changes);
    }

    private void compare(DataResource tag, Tag before, Tag after, List<TagChange> changes) {
        TreeSet<String> added = new TreeSet<>(after.values());
        added.removeAll(before.values());
        TreeSet<String> removed = new TreeSet<>(before.values());
        removed.removeAll(after.values());
        if (!added.isEmpty()) {
            changes.add(new TagValuesAdded(tag, List.copyOf(added)));
        }
        if (!removed.isEmpty()) {
            changes.add(new TagValuesRemoved(tag, List.copyOf(removed)));
        }
        if (before.replace() != after.replace()) {
            changes.add(new TagReplaceModified(tag, before.replace(), after.replace()));
        }
    }
}
