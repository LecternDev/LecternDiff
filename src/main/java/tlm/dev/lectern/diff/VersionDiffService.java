package tlm.dev.lectern.diff;

import java.util.ArrayList;
import java.util.List;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.model.Skipped;
import tlm.dev.lectern.diff.read.VersionTree;

public final class VersionDiffService {

    private final List<VersionComparison> comparisons;

    public VersionDiffService(List<VersionComparison> comparisons) {
        this.comparisons = List.copyOf(comparisons);
    }

    public ChangeSet diff(VersionTree from, VersionTree to) {
        List<Change> changes = new ArrayList<>();
        List<Skipped> skipped = new ArrayList<>();
        for (VersionComparison comparison : comparisons) {
            VersionComparison.Outcome outcome = comparison.compare(from, to);
            changes.addAll(outcome.changes());
            skipped.addAll(outcome.skipped());
        }
        return new ChangeSet(from.version(), to.version(), skipped, changes);
    }
}
