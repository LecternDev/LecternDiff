package tlm.dev.lectern.diff.read;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.UncheckedIOException;
import java.nio.file.LinkOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
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
    public List<String> files(String directory) {
        Path start = root.resolve(directory).normalize();
        if (!start.startsWith(root)) {
            throw new VersionReadException("Version " + version.id() + ": " + directory + " leaves the version folder");
        }
        if (!Files.isDirectory(start)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(start)) {
            return walk.filter(path -> !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                    .map(this::regularFile)
                    .sorted()
                    .toList();
        } catch (IOException | UncheckedIOException e) {
            throw new VersionReadException("Version " + version.id() + ", " + directory + ": cannot list the files: " + e, e);
        }
    }

    private String regularFile(Path path) {
        String relative = root.relativize(path).toString().replace('\\', '/');
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new VersionReadException("Version " + version.id() + ", " + relative + ": not a regular file");
        }
        return relative;
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
