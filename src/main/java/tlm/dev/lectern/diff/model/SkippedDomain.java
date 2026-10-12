package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record SkippedDomain(ChangeDomain domain, SkipSide side, SkipReason reason) {

    public SkippedDomain {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(reason, "reason");
    }
}
