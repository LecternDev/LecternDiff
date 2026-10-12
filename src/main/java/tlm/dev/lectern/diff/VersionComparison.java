package tlm.dev.lectern.diff;

import java.util.List;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.Skipped;
import tlm.dev.lectern.diff.read.VersionTree;

public interface VersionComparison {

    Outcome compare(VersionTree from, VersionTree to);

    record Outcome(List<Change> changes, List<Skipped> skipped) {

        public Outcome {
            changes = List.copyOf(changes);
            skipped = List.copyOf(skipped);
        }
    }
}
