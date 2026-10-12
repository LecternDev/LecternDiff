package tlm.dev.lectern.diff.jsondiff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.json.JsonValue;

class JsonComparerTest {

    private final JsonReader json = new JsonReader();
    private final JsonComparer comparer = new JsonComparer();

    private JsonValue value(String text) {
        return json.read(text);
    }

    private List<PlaceChange> compare(String from, String to) {
        return comparer.compare(value(from), value(to));
    }

    private static JsonPointer pointer(String... tokens) {
        return new JsonPointer(List.of(tokens));
    }

    @Test
    void identicalValuesGiveNoPlace() {
        assertEquals(List.of(), compare("{\"a\": [1, {\"b\": null}], \"c\": \"x\"}", "{\"c\": \"x\", \"a\": [1, {\"b\": null}]}"));
    }

    @Test
    void theOrderOfTheObjectMembersIsNotAChange() {
        assertEquals(List.of(), compare("{\"a\": 1, \"b\": 2, \"c\": 3}", "{\"c\": 3, \"a\": 1, \"b\": 2}"));
    }

    @Test
    void aScalarThatChangesIsOneModifiedPlaceWithBothValues() {
        assertEquals(List.of(new PlaceChange.Modified(pointer("gadget", "size"), value("2"), value("3"))),
                compare("{\"gadget\": {\"size\": 2, \"name\": \"demo\"}}", "{\"gadget\": {\"size\": 3, \"name\": \"demo\"}}"));
    }

    @Test
    void numbersAreComparedByTheirWrittenText() {
        assertEquals(List.of(new PlaceChange.Modified(pointer("n"), value("1"), value("1.0"))), compare("{\"n\": 1}", "{\"n\": 1.0}"));
        assertEquals(List.of(new PlaceChange.Modified(pointer("n"), value("0.50"), value("0.5"))),
                compare("{\"n\": 0.50}", "{\"n\": 0.5}"));
        assertEquals(List.of(), compare("{\"n\": 1.0}", "{\"n\": 1.0}"));
    }

    @Test
    void aStringAndANumberWithTheSameDigitsAreDifferent() {
        assertEquals(1, compare("{\"n\": 1}", "{\"n\": \"1\"}").size());
    }

    @Test
    void anAddedMemberIsOnePlaceAtItsRootWhateverItsDepth() {
        assertEquals(List.of(new PlaceChange.Added(pointer("effects"), value("{\"a\": {\"b\": [1, 2, {\"c\": 3}]}}"))),
                compare("{\"id\": \"demo\"}", "{\"id\": \"demo\", \"effects\": {\"a\": {\"b\": [1, 2, {\"c\": 3}]}}}"));
    }

    @Test
    void aRemovedMemberIsOnePlaceAtItsRootWhateverItsDepth() {
        assertEquals(List.of(new PlaceChange.Removed(pointer("pools"), value("[{\"entries\": [{\"name\": \"demo:item\"}]}]"))),
                compare("{\"pools\": [{\"entries\": [{\"name\": \"demo:item\"}]}]}", "{}"));
    }

    @Test
    void placesAreListedInDocumentOrderWithMembersSortedByName() {
        List<PlaceChange> places = compare("{\"b\": 1, \"a\": {\"y\": 1, \"x\": 1}, \"c\": 1}",
                "{\"b\": 2, \"a\": {\"y\": 2, \"x\": 2}, \"c\": 2, \"d\": 2}");
        assertEquals(List.of("/a/x", "/a/y", "/b", "/c", "/d"), places.stream().map(place -> place.pointer().text()).toList());
    }

    @Test
    void arraysAreComparedElementByElementByIndex() {
        assertEquals(List.of(new PlaceChange.Modified(pointer("rows", "1"), value("\"# #\""), value("\"###\""))),
                compare("{\"rows\": [\"###\", \"# #\"]}", "{\"rows\": [\"###\", \"###\"]}"));
    }

    @Test
    void elementsAddedAtTheEndOfAnArrayAreOnePlaceEach() {
        assertEquals(
                List.of(new PlaceChange.Added(pointer("rows", "2"), value("\"c\"")), new PlaceChange.Added(pointer("rows", "3"), value("\"d\""))),
                compare("{\"rows\": [\"a\", \"b\"]}", "{\"rows\": [\"a\", \"b\", \"c\", \"d\"]}"));
    }

    @Test
    void elementsRemovedFromTheEndOfAnArrayAreOnePlaceEach() {
        assertEquals(List.of(new PlaceChange.Removed(pointer("rows", "1"), value("\"b\""))), compare("{\"rows\": [\"a\", \"b\"]}", "{\"rows\": [\"a\"]}"));
    }

    @Test
    void anObjectThatBecomesAnArrayIsOneModifiedPlaceWithTheWholeValues() {
        assertEquals(List.of(new PlaceChange.Modified(pointer("ingredient"), value("{\"item\": \"demo:a\"}"), value("[{\"item\": \"demo:a\"}]"))),
                compare("{\"ingredient\": {\"item\": \"demo:a\"}}", "{\"ingredient\": [{\"item\": \"demo:a\"}]}"));
    }

    @Test
    void aWholeValueThatChangesKindIsModifiedAtTheRoot() {
        assertEquals(List.of(new PlaceChange.Modified(JsonPointer.root(), value("[1]"), value("{\"a\": 1}"))), compare("[1]", "{\"a\": 1}"));
    }

    @Test
    void nullAndMissingAreDifferent() {
        assertEquals(List.of(new PlaceChange.Modified(pointer("a"), value("null"), value("false"))), compare("{\"a\": null}", "{\"a\": false}"));
        assertEquals(List.of(new PlaceChange.Added(pointer("a"), value("null"))), compare("{}", "{\"a\": null}"));
    }

    @Test
    void membersWithSlashesAndTildesInTheirNamesAreEscapedInTheTextOfThePointer() {
        List<PlaceChange> places = compare("{\"a/b\": 1, \"c~d\": 1}", "{\"a/b\": 2, \"c~d\": 2}");
        assertEquals(List.of("/a~1b", "/c~0d"), places.stream().map(place -> place.pointer().text()).toList());
    }

    @Test
    void comparingTwiceGivesTheSamePlaces() {
        String from = "{\"a\": [1, 2, 3], \"b\": {\"c\": 1}}";
        String to = "{\"a\": [1, 5], \"b\": {\"c\": 1, \"d\": 2}}";
        assertEquals(compare(from, to), compare(from, to));
    }
}
