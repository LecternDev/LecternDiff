package tlm.dev.lectern.diff.tags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.tag;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.model.DataFiles;
import tlm.dev.lectern.diff.model.DataResource;
import tlm.dev.lectern.diff.model.Tag;
import tlm.dev.lectern.diff.model.TagAdded;
import tlm.dev.lectern.diff.model.TagRemoved;
import tlm.dev.lectern.diff.model.TagReplaceModified;
import tlm.dev.lectern.diff.model.TagValuesAdded;
import tlm.dev.lectern.diff.model.TagValuesRemoved;

class TagDifferTest {

    private final TagDiffer differ = new TagDiffer();

    private static Tag values(String... values) {
        return new Tag(false, Set.copyOf(Arrays.asList(values)));
    }

    private List<Change> diff(Map<DataResource, Tag> from, Map<DataResource, Tag> to) {
        return new ChangeSet(release("1.0"), release("1.1"), List.copyOf(differ.diff(new DataFiles<>(from), new DataFiles<>(to)))).changes();
    }

    @Test
    void identicalTagsGiveNoChange() {
        Map<DataResource, Tag> same = Map.of(tag("block", "logs"), values("demo:a", "demo:b"));
        assertEquals(List.of(), diff(same, same));
    }

    @Test
    void anAddedTagIsOneChangeWithItsNumberOfDistinctValuesAndNeverTheValues() {
        assertEquals(List.of(new TagAdded(tag("block", "logs"), 2)),
                diff(Map.of(), Map.of(tag("block", "logs"), values("demo:a", "demo:b", "demo:a"))));
    }

    @Test
    void aRemovedTagIsOneChangeWithTheNumberOfValuesOfTheOldVersion() {
        assertEquals(List.of(new TagRemoved(tag("item", "tools"), 3)),
                diff(Map.of(tag("item", "tools"), values("demo:a", "demo:b", "#demo:c")), Map.of()));
    }

    @Test
    void valuesAddedAndRemovedAreTwoChangesWithSortedValues() {
        assertEquals(List.of(new TagValuesAdded(tag("block", "logs"), List.of("demo:c", "demo:d")),
                        new TagValuesRemoved(tag("block", "logs"), List.of("#demo:b"))),
                diff(Map.of(tag("block", "logs"), values("demo:a", "#demo:b")),
                        Map.of(tag("block", "logs"), values("demo:a", "demo:d", "demo:c"))));
    }

    @Test
    void theSameValuesInAnotherOrderAreNotAChange() {
        assertEquals(List.of(), diff(Map.of(tag("block", "logs"), values("demo:b", "demo:a")),
                Map.of(tag("block", "logs"), values("demo:a", "demo:b"))));
    }

    @Test
    void aValueRepeatedInATagIsNotAChange() {
        assertEquals(List.of(), diff(Map.of(tag("block", "logs"), new Tag(false, Set.of("demo:a"))),
                Map.of(tag("block", "logs"), values("demo:a", "demo:a"))));
    }

    @Test
    void aTagReferenceAndAPlainValueWithTheSameNameAreDifferentValues() {
        assertEquals(List.of(new TagValuesAdded(tag("block", "logs"), List.of("demo:a")),
                        new TagValuesRemoved(tag("block", "logs"), List.of("#demo:a"))),
                diff(Map.of(tag("block", "logs"), values("#demo:a")), Map.of(tag("block", "logs"), values("demo:a"))));
    }

    @Test
    void theReplaceFlagThatChangesIsOneChange() {
        assertEquals(List.of(new TagReplaceModified(tag("fluid", "water"), false, true)),
                diff(Map.of(tag("fluid", "water"), new Tag(false, Set.of("demo:a"))), Map.of(tag("fluid", "water"), new Tag(true, Set.of("demo:a")))));
    }

    @Test
    void valuesAndTheReplaceFlagCanChangeTogetherAndGiveOneChangeEach() {
        assertEquals(3, diff(Map.of(tag("fluid", "water"), new Tag(false, Set.of("demo:a"))),
                Map.of(tag("fluid", "water"), new Tag(true, Set.of("demo:b")))).size());
    }

    @Test
    void sameNameInAnotherRegistryIsAnotherTag() {
        assertEquals(List.of(new TagRemoved(tag("block", "logs"), 1), new TagAdded(tag("item", "logs"), 1)),
                diff(Map.of(tag("block", "logs"), values("demo:a")), Map.of(tag("item", "logs"), values("demo:a"))));
    }
}
