package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record SkippedDomain(ChangeDomain domain, SkipSide side, SkipReason reason) implements Skipped {

    public SkippedDomain {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(reason, "reason");
        if (!(reason instanceof SkipReason.ReportMissing)) {
            throw new IllegalArgumentException("A skipped domain misses its report: " + reason);
        }
    }
}
