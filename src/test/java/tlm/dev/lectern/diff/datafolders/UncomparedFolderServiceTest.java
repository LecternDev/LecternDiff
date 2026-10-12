package tlm.dev.lectern.diff.datafolders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.json;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.DataFiles;
import tlm.dev.lectern.diff.model.DataResource;
import tlm.dev.lectern.diff.model.SkippedFolder;

class UncomparedFolderServiceTest {

    private final UncomparedFolderService service = new UncomparedFolderService();

    private static DataFiles<JsonValue> files(Object... pairs) {
        Map<DataResource, JsonValue> files = new HashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            files.put((DataResource) pairs[index], json((String) pairs[index + 1]));
        }
        return new DataFiles<>(files);
    }

    @Test
    void identicalFoldersGiveNothing() {
        DataFiles<JsonValue> same = files(resource("demo", "worldgen", "biome/a"), "{\"x\": 1}");
        assertEquals(List.of(), service.summarize(same, same));
    }

    @Test
    void addedRemovedAndModifiedFilesAreCountedPerFolderAndSortedByFolderName() {
        DataFiles<JsonValue> from = files(
                resource("demo", "worldgen", "a"), "{\"x\": 1}", resource("demo", "worldgen", "b"), "{\"x\": 1}",
                resource("demo", "worldgen", "c"), "{\"x\": 1}", resource("demo", "advancement", "p"), "{}",
                resource("demo", "advancement", "q"), "{}");
        DataFiles<JsonValue> to = files(
                resource("demo", "worldgen", "a"), "{\"x\": 2}", resource("demo", "worldgen", "b"), "{\"x\": 1}",
                resource("demo", "worldgen", "d"), "{}", resource("demo", "worldgen", "e"), "{}",
                resource("demo", "advancement", "p"), "{}", resource("demo", "block_sound_set", "z"), "{}");
        assertEquals(List.of(new SkippedFolder("advancement", 0, 1, 0), new SkippedFolder("block_sound_set", 1, 0, 0),
                new SkippedFolder("worldgen", 2, 1, 1)), service.summarize(from, to));
    }

    @Test
    void aFolderIsCountedOverEveryNamespace() {
        DataFiles<JsonValue> from = files();
        DataFiles<JsonValue> to = files(resource("demo", "dialog", "a"), "{}", resource("other", "dialog", "a"), "{}");
        assertEquals(List.of(new SkippedFolder("dialog", 2, 0, 0)), service.summarize(from, to));
    }

    @Test
    void theOrderOfTheKeysOfAFileIsNotAModification() {
        DataFiles<JsonValue> from = files(resource("demo", "worldgen", "a"), "{\"x\": 1, \"y\": [1, 2]}");
        DataFiles<JsonValue> to = files(resource("demo", "worldgen", "a"), "{\"y\": [1, 2], \"x\": 1}");
        assertEquals(List.of(), service.summarize(from, to));
    }

    @Test
    void aNumberWrittenDifferentlyIsAModification() {
        DataFiles<JsonValue> from = files(resource("demo", "worldgen", "a"), "{\"x\": 1}");
        DataFiles<JsonValue> to = files(resource("demo", "worldgen", "a"), "{\"x\": 1.0}");
        assertEquals(List.of(new SkippedFolder("worldgen", 0, 0, 1)), service.summarize(from, to));
    }

    @Test
    void theTopFolderOfANestedDatapackIsTheFolder() {
        DataFiles<JsonValue> from = files();
        DataFiles<JsonValue> to = files(resource("demo", "datapacks", "pack/data/demo/recipe/a"), "{}", resource("demo", "datapacks", "pack/pack.mcmeta"), "{}");
        assertEquals(List.of(new SkippedFolder("datapacks", 2, 0, 0)), service.summarize(from, to));
    }
}
