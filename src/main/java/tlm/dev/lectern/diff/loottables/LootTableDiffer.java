package tlm.dev.lectern.diff.loottables;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tlm.dev.lectern.diff.jsondiff.JsonComparer;
import tlm.dev.lectern.diff.jsondiff.PlaceChange;
import tlm.dev.lectern.diff.model.DataFiles;
import tlm.dev.lectern.diff.model.DataResource;
import tlm.dev.lectern.diff.model.LootTable;
import tlm.dev.lectern.diff.model.LootTableAdded;
import tlm.dev.lectern.diff.model.LootTableChange;
import tlm.dev.lectern.diff.model.LootTableModified;
import tlm.dev.lectern.diff.model.LootTableRemoved;

public final class LootTableDiffer {

    private final JsonComparer comparer;

    public LootTableDiffer(JsonComparer comparer) {
        this.comparer = comparer;
    }

    public List<LootTableChange> diff(DataFiles<LootTable> from, DataFiles<LootTable> to) {
        List<LootTableChange> changes = new ArrayList<>();
        for (Map.Entry<DataResource, LootTable> entry : from.files().entrySet()) {
            LootTable before = entry.getValue();
            LootTable after = to.files().get(entry.getKey());
            if (after == null) {
                changes.add(new LootTableRemoved(entry.getKey(), before.type()));
                continue;
            }
            List<PlaceChange> places = comparer.compare(before.content(), after.content());
            if (!places.isEmpty()) {
                changes.add(new LootTableModified(entry.getKey(), places));
            }
        }
        for (Map.Entry<DataResource, LootTable> entry : to.files().entrySet()) {
            if (!from.files().containsKey(entry.getKey())) {
                changes.add(new LootTableAdded(entry.getKey(), entry.getValue().type()));
            }
        }
        return List.copyOf(changes);
    }
}
