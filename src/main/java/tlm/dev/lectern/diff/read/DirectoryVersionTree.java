package tlm.dev.lectern.diff.read;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import tlm.dev.lectern.diff.model.VersionRef;

public final class DirectoryVersionTree implements VersionTree {

    private final Path root;
    private final VersionRef version;

    public DirectoryVersionTree(Path root, VersionRef version) {
        Path absolute = root.toAbsolutePath().normalize();
        if (!Files.isDirectory(absolute)) {
            throw new VersionReadException("Not a version folder: " + absolute);
        }
        this.root = absolute;
        this.version = version;
    }

    @Override
    public VersionRef version() {
        return version;
    }

    @Override
    public Optional<String> readText(String relativePath) {
        Path file = root.resolve(relativePath).normalize();
        if (!file.startsWith(root)) {
            throw new VersionReadException("Version " + version.id() + ": " + relativePath + " leaves the version folder");
        }
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new VersionReadException("Version " + version.id() + ", " + relativePath + ": cannot read the file: " + e, e);
        }
    }
}
