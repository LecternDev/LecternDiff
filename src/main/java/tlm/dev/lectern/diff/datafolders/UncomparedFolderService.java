package tlm.dev.lectern.diff.datafolders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.DataFiles;
import tlm.dev.lectern.diff.model.DataResource;
import tlm.dev.lectern.diff.model.SkippedFolder;

public final class UncomparedFolderService {

    private static final class Counts {

        private int added;
        private int removed;
        private int modified;
    }

    public List<SkippedFolder> summarize(DataFiles<JsonValue> from, DataFiles<JsonValue> to) {
        Map<String, Counts> byFolder = new TreeMap<>();
        for (Map.Entry<DataResource, JsonValue> entry : from.files().entrySet()) {
            Counts counts = byFolder.computeIfAbsent(entry.getKey().kind().folder(), folder -> new Counts());
            JsonValue after = to.files().get(entry.getKey());
            if (after == null) {
                counts.removed++;
            } else if (!after.equals(entry.getValue())) {
                counts.modified++;
            }
        }
        for (DataResource resource : to.files().keySet()) {
            if (!from.files().containsKey(resource)) {
                byFolder.computeIfAbsent(resource.kind().folder(), folder -> new Counts()).added++;
            }
        }
        List<SkippedFolder> changed = new ArrayList<>();
        byFolder.forEach((folder, counts) -> {
            if (counts.added + counts.removed + counts.modified > 0) {
                changed.add(new SkippedFolder(folder, counts.added, counts.removed, counts.modified));
            }
        });
        return List.copyOf(changed);
    }
}
