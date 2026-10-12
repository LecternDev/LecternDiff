package tlm.dev.lectern.diff;

import java.util.ArrayList;
import java.util.List;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.model.SkippedDomain;
import tlm.dev.lectern.diff.read.VersionTree;

public final class VersionDiffService {

    private final List<DomainComparison<?>> comparisons;

    public VersionDiffService(List<DomainComparison<?>> comparisons) {
        this.comparisons = List.copyOf(comparisons);
    }

    public ChangeSet diff(VersionTree from, VersionTree to) {
        List<Change> changes = new ArrayList<>();
        List<SkippedDomain> skipped = new ArrayList<>();
        for (DomainComparison<?> comparison : comparisons) {
            DomainComparison.Outcome outcome = comparison.compare(from, to);
            changes.addAll(outcome.changes());
            outcome.skipped().ifPresent(skipped::add);
        }
        return new ChangeSet(from.version(), to.version(), skipped, changes);
    }
}
