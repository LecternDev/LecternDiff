package tlm.dev.lectern.diff.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tlm.dev.lectern.diff.model.VersionKind;

class DirectoryVersionTreeTest {

    private final VersionKindReader kindReader = new VersionKindReader();

    @TempDir
    Path workDirectory;

    private Path versionFolder(String name) throws IOException {
        return Files.createDirectories(workDirectory.resolve(name));
    }

    @Test
    void theVersionComesFromTheFolderName() throws IOException {
        DirectoryVersionTree tree = new DirectoryVersionTree(versionFolder("26.4-snapshot-3"), kindReader);
        assertEquals("26.4-snapshot-3", tree.version().id().value());
        assertEquals(new VersionKind.Snapshot(), tree.version().kind());
    }

    @Test
    void readsAFileRelativeToTheVersionFolder() throws IOException {
        Path folder = versionFolder("1.0");
        Files.createDirectories(folder.resolve("reports"));
        Files.writeString(folder.resolve("reports/registries.json"), "{}", StandardCharsets.UTF_8);
        assertEquals(Optional.of("{}"), new DirectoryVersionTree(folder, kindReader).readText("reports/registries.json"));
    }

    @Test
    void aMissingFileIsAbsentNotAnError() throws IOException {
        assertEquals(Optional.empty(), new DirectoryVersionTree(versionFolder("1.0"), kindReader).readText("reports/registries.json"));
    }

    @Test
    void aFolderThatDoesNotExistIsRefused() {
        VersionReadException failure = assertThrows(VersionReadException.class,
                () -> new DirectoryVersionTree(workDirectory.resolve("1.0"), kindReader));
        assertTrue(failure.getMessage().startsWith("Not a version folder: "), failure.getMessage());
    }

    @Test
    void aFolderWhoseNameIsNotAVersionIdIsRefused() throws IOException {
        Path folder = versionFolder("not-a-version");
        VersionReadException failure = assertThrows(VersionReadException.class, () -> new DirectoryVersionTree(folder, kindReader));
        assertTrue(failure.getMessage().contains("Cannot tell the kind of version 'not-a-version'"), failure.getMessage());
    }

    @Test
    void aPathThatLeavesTheVersionFolderIsRefused() throws IOException {
        DirectoryVersionTree tree = new DirectoryVersionTree(versionFolder("1.0"), kindReader);
        VersionReadException failure = assertThrows(VersionReadException.class, () -> tree.readText("../secret.txt"));
        assertTrue(failure.getMessage().contains("leaves the version folder"), failure.getMessage());
    }

    @Test
    void aFileThatIsNotValidUtf8IsRefusedWithTheVersionAndTheFile() throws IOException {
        Path folder = versionFolder("1.0");
        Files.write(folder.resolve("broken.json"), new byte[] {(byte) 0xC3, (byte) 0x28});
        DirectoryVersionTree tree = new DirectoryVersionTree(folder, kindReader);
        VersionReadException failure = assertThrows(VersionReadException.class, () -> tree.readText("broken.json"));
        assertTrue(failure.getMessage().startsWith("Version 1.0, broken.json: "), failure.getMessage());
    }
}
