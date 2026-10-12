package tlm.dev.lectern.diff.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.snapshot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DirectoryVersionTreeTest {

    @TempDir
    Path workDirectory;

    private Path folder(String name) throws IOException {
        return Files.createDirectories(workDirectory.resolve(name));
    }

    @Test
    void theVersionIsTheOneGivenByTheCallerWhateverTheFolderName() throws IOException {
        DirectoryVersionTree tree = new DirectoryVersionTree(folder("anything"), snapshot("26.4-snapshot-3"));
        assertEquals(snapshot("26.4-snapshot-3"), tree.version());
    }

    @Test
    void readsAFileRelativeToTheVersionFolder() throws IOException {
        Path folder = folder("1.0");
        Files.createDirectories(folder.resolve("reports"));
        Files.writeString(folder.resolve("reports/registries.json"), "{}", StandardCharsets.UTF_8);
        assertEquals(Optional.of("{}"),
                new DirectoryVersionTree(folder, release("1.0")).readText("reports/registries.json"));
    }

    @Test
    void filesListsEveryFileBelowADirectoryAsSortedRelativePathsWithSlashes() throws IOException {
        Path folder = folder("1.0");
        Files.createDirectories(folder.resolve("data/demo/recipe/brewing"));
        Files.writeString(folder.resolve("data/demo/recipe/b.json"), "{}", StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("data/demo/recipe/a.json"), "{}", StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("data/demo/recipe/brewing/c.json"), "{}", StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("reports.json"), "{}", StandardCharsets.UTF_8);
        assertEquals(List.of("data/demo/recipe/a.json", "data/demo/recipe/b.json", "data/demo/recipe/brewing/c.json"),
                new DirectoryVersionTree(folder, release("1.0")).files("data"));
    }

    @Test
    void filesOfADirectoryThatDoesNotExistIsEmpty() throws IOException {
        assertEquals(List.of(), new DirectoryVersionTree(folder("1.0"), release("1.0")).files("data"));
    }

    @Test
    void filesOfAFileThatIsNotADirectoryIsEmpty() throws IOException {
        Path folder = folder("1.0");
        Files.writeString(folder.resolve("data"), "{}", StandardCharsets.UTF_8);
        assertEquals(List.of(), new DirectoryVersionTree(folder, release("1.0")).files("data"));
    }

    @Test
    void filesOfADirectoryOutsideTheVersionFolderIsRefused() throws IOException {
        DirectoryVersionTree tree = new DirectoryVersionTree(folder("1.0"), release("1.0"));
        VersionReadException failure = assertThrows(VersionReadException.class, () -> tree.files("../other"));
        assertTrue(failure.getMessage().contains("leaves the version folder"), failure.getMessage());
    }

    @Test
    void aSymbolicLinkInsideADirectoryIsRefusedWhenTheSystemAllowsOne() throws IOException {
        Path folder = folder("1.0");
        Files.createDirectories(folder.resolve("data"));
        Path target = Files.writeString(workDirectory.resolve("target.json"), "{}", StandardCharsets.UTF_8);
        try {
            Files.createSymbolicLink(folder.resolve("data/link.json"), target);
        } catch (IOException | UnsupportedOperationException e) {
            assumeTrue(false, "Symbolic links cannot be created here: " + e);
        }
        DirectoryVersionTree tree = new DirectoryVersionTree(folder, release("1.0"));
        VersionReadException failure = assertThrows(VersionReadException.class, () -> tree.files("data"));
        assertTrue(failure.getMessage().contains("not a regular file"), failure.getMessage());
    }

    @Test
    void aMissingFileIsAbsentNotAnError() throws IOException {
        assertEquals(Optional.empty(),
                new DirectoryVersionTree(folder("1.0"), release("1.0")).readText("reports/registries.json"));
    }

    @Test
    void aFolderThatDoesNotExistIsRefused() {
        VersionReadException failure = assertThrows(VersionReadException.class,
                () -> new DirectoryVersionTree(workDirectory.resolve("1.0"), release("1.0")));
        assertTrue(failure.getMessage().startsWith("Not a version folder: "), failure.getMessage());
    }

    @Test
    void aPathThatLeavesTheVersionFolderIsRefused() throws IOException {
        DirectoryVersionTree tree = new DirectoryVersionTree(folder("1.0"), release("1.0"));
        VersionReadException failure = assertThrows(VersionReadException.class, () -> tree.readText("../secret.txt"));
        assertTrue(failure.getMessage().contains("leaves the version folder"), failure.getMessage());
    }

    @Test
    void aFileThatIsNotValidUtf8IsRefusedWithTheVersionAndTheFile() throws IOException {
        Path folder = folder("1.0");
        Files.write(folder.resolve("broken.json"), new byte[] {(byte) 0xC3, (byte) 0x28});
        DirectoryVersionTree tree = new DirectoryVersionTree(folder, release("1.0"));
        VersionReadException failure = assertThrows(VersionReadException.class, () -> tree.readText("broken.json"));
        assertTrue(failure.getMessage().startsWith("Version 1.0, broken.json: "), failure.getMessage());
    }
}
