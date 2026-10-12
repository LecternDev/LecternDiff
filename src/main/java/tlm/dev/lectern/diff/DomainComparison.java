package tlm.dev.lectern.diff;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeDomain;
import tlm.dev.lectern.diff.model.SkipReason;
import tlm.dev.lectern.diff.model.SkipSide;
import tlm.dev.lectern.diff.model.SkippedDomain;
import tlm.dev.lectern.diff.read.VersionTree;

public record DomainComparison<T>(
        ChangeDomain domain,
        Function<VersionTree, Optional<T>> reader,
        BiFunction<T, T, ? extends List<? extends Change>> differ) implements VersionComparison {

    @Override
    public Outcome compare(VersionTree from, VersionTree to) {
        Optional<T> before = reader.apply(from);
        Optional<T> after = reader.apply(to);
        if (before.isPresent() && after.isPresent()) {
            return new Outcome(List.copyOf(differ.apply(before.get(), after.get())), List.of());
        }
        return new Outcome(List.of(), List.of(new SkippedDomain(domain, missingSide(before, after), new SkipReason.ReportMissing())));
    }

    private static SkipSide missingSide(Optional<?> before, Optional<?> after) {
        if (before.isEmpty() && after.isEmpty()) {
            return SkipSide.BOTH;
        }
        return before.isEmpty() ? SkipSide.FROM : SkipSide.TO;
    }
}
