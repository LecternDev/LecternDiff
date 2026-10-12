package tlm.dev.lectern.diff;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import tlm.dev.lectern.diff.datafolders.UncomparedFolderService;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.DataFiles;
import tlm.dev.lectern.diff.read.VersionTree;

public record UncomparedFoldersComparison(
        Function<VersionTree, Optional<DataFiles<JsonValue>>> reader, UncomparedFolderService service)
        implements VersionComparison {

    @Override
    public Outcome compare(VersionTree from, VersionTree to) {
        Optional<DataFiles<JsonValue>> before = reader.apply(from);
        Optional<DataFiles<JsonValue>> after = reader.apply(to);
        if (before.isPresent() && after.isPresent()) {
            return new Outcome(List.of(), List.copyOf(service.summarize(before.get(), after.get())));
        }
        return new Outcome(List.of(), List.of());
    }
}
