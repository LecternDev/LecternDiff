package tlm.dev.lectern.diff.loottables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.json;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.lootTable;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.jsondiff.JsonComparer;
import tlm.dev.lectern.diff.jsondiff.JsonPointer;
import tlm.dev.lectern.diff.jsondiff.PlaceChange;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.model.DataFiles;
import tlm.dev.lectern.diff.model.DataResource;
import tlm.dev.lectern.diff.model.LootTable;
import tlm.dev.lectern.diff.model.LootTableAdded;
import tlm.dev.lectern.diff.model.LootTableModified;
import tlm.dev.lectern.diff.model.LootTableRemoved;

class LootTableDifferTest {

    private final LootTableDiffer differ = new LootTableDiffer(new JsonComparer());

    private static LootTable table(String type, String content) {
        return new LootTable(Optional.ofNullable(type), json(content));
    }

    private List<Change> diff(Map<DataResource, LootTable> from, Map<DataResource, LootTable> to) {
        return new ChangeSet(release("1.0"), release("1.1"),
                List.copyOf(differ.diff(new DataFiles<>(from), new DataFiles<>(to)))).changes();
    }

    @Test
    void identicalLootTablesGiveNoChange() {
        Map<DataResource, LootTable> same = Map.of(lootTable("a"), table("demo:chest", "{\"type\": \"demo:chest\", \"pools\": []}"));
        assertEquals(List.of(), diff(same, same));
    }

    @Test
    void anAddedLootTableIsOneChangeWithItsTypeAndNeverItsPools() {
        assertEquals(List.of(new LootTableAdded(lootTable("b"), Optional.of("demo:chest"))),
                diff(Map.of(), Map.of(lootTable("b"), table("demo:chest", "{\"type\": \"demo:chest\", \"pools\": [{\"rolls\": 1}]}"))));
    }

    @Test
    void aRemovedLootTableWithoutATypeKeepsAnEmptyType() {
        assertEquals(List.of(new LootTableRemoved(lootTable("b"), Optional.empty())),
                diff(Map.of(lootTable("b"), table(null, "{\"pools\": []}")), Map.of()));
    }

    @Test
    void aModifiedLootTableListsTheAddedAndRemovedPoolsAndTheChangedValues() {
        LootTable before = table("demo:chest", "{\"type\": \"demo:chest\", \"pools\": [{\"rolls\": 1}], \"random_sequence\": \"demo:a\"}");
        LootTable after = table("demo:chest", "{\"type\": \"demo:chest\", \"pools\": [{\"rolls\": 2}, {\"rolls\": 1}]}");
        assertEquals(List.of(new LootTableModified(lootTable("a"), List.of(
                        new PlaceChange.Modified(JsonPointer.root().child("pools").child(0).child("rolls"), json("1"), json("2")),
                        new PlaceChange.Added(JsonPointer.root().child("pools").child(1), json("{\"rolls\": 1}")),
                        new PlaceChange.Removed(JsonPointer.root().child("random_sequence"), json("\"demo:a\""))))),
                diff(Map.of(lootTable("a"), before), Map.of(lootTable("a"), after)));
    }

    @Test
    void theOrderOfTheKeysOfALootTableIsNotAChange() {
        LootTable before = table(null, "{\"a\": 1, \"b\": 2}");
        LootTable after = table(null, "{\"b\": 2, \"a\": 1}");
        assertEquals(List.of(), diff(Map.of(lootTable("a"), before), Map.of(lootTable("a"), after)));
    }
}
