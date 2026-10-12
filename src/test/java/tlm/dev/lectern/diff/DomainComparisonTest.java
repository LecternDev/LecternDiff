package tlm.dev.lectern.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.blockKey;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.fixtures.InMemoryVersionTree;
import tlm.dev.lectern.diff.model.BlockAdded;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeDomain;
import tlm.dev.lectern.diff.model.SkipReason;
import tlm.dev.lectern.diff.model.SkipSide;
import tlm.dev.lectern.diff.model.SkippedDomain;
import tlm.dev.lectern.diff.read.VersionReadException;
import tlm.dev.lectern.diff.read.VersionTree;

class DomainComparisonTest {

    private static final Change CHANGE = new BlockAdded(blockKey("demo:a"), 1);

    private final DomainComparison<String> comparison = new DomainComparison<>(
            ChangeDomain.BLOCKS,
            tree -> tree.readText("report").map(text -> {
                if (text.equals("broken")) {
                    throw new VersionReadException("Version " + tree.version().id() + ", report: unreadable");
                }
                return text;
            }),
            (before, after) -> before.equals(after) ? List.of() : List.of(CHANGE));

    private static VersionTree with(String id, String report) {
        return new InMemoryVersionTree(release(id), Map.of("report", report));
    }

    private static VersionTree without(String id) {
        return new InMemoryVersionTree(release(id), Map.of());
    }

    private static Optional<SkippedDomain> skipped(SkipSide side) {
        return Optional.of(new SkippedDomain(ChangeDomain.BLOCKS, side, new SkipReason.ReportMissing()));
    }

    @Test
    void bothReportsPresentGiveTheChangesAndNothingSkipped() {
        DomainComparison.Outcome outcome = comparison.compare(with("1.0", "a"), with("1.1", "b"));
        assertEquals(List.of(CHANGE), outcome.changes());
        assertEquals(Optional.empty(), outcome.skipped());
    }

    @Test
    void bothReportsPresentAndEqualGiveNoChangeAndNothingSkipped() {
        DomainComparison.Outcome outcome = comparison.compare(with("1.0", "a"), with("1.1", "a"));
        assertEquals(List.of(), outcome.changes());
        assertEquals(Optional.empty(), outcome.skipped());
    }

    @Test
    void aReportMissingFromTheFirstVersionSkipsTheDomainOnTheFromSide() {
        DomainComparison.Outcome outcome = comparison.compare(without("1.0"), with("1.1", "b"));
        assertEquals(List.of(), outcome.changes());
        assertEquals(skipped(SkipSide.FROM), outcome.skipped());
    }

    @Test
    void aReportMissingFromTheSecondVersionSkipsTheDomainOnTheToSide() {
        DomainComparison.Outcome outcome = comparison.compare(with("1.0", "a"), without("1.1"));
        assertEquals(List.of(), outcome.changes());
        assertEquals(skipped(SkipSide.TO), outcome.skipped());
    }

    @Test
    void aReportMissingFromBothVersionsSkipsTheDomainOnBothSides() {
        DomainComparison.Outcome outcome = comparison.compare(without("1.0"), without("1.1"));
        assertEquals(List.of(), outcome.changes());
        assertEquals(skipped(SkipSide.BOTH), outcome.skipped());
    }

    @Test
    void anUnreadableReportIsAnErrorEvenWhenTheOtherVersionHasNoReport() {
        VersionReadException onTheSecond = assertThrows(VersionReadException.class,
                () -> comparison.compare(without("1.0"), with("1.1", "broken")));
        assertEquals("Version 1.1, report: unreadable", onTheSecond.getMessage());
        VersionReadException onTheFirst = assertThrows(VersionReadException.class,
                () -> comparison.compare(with("1.0", "broken"), without("1.1")));
        assertEquals("Version 1.0, report: unreadable", onTheFirst.getMessage());
    }
}
