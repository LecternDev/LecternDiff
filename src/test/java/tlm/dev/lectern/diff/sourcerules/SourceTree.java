package tlm.dev.lectern.diff.sourcerules;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

final class SourceTree {

    private final Path projectDirectory;

    SourceTree(Path projectDirectory) {
        this.projectDirectory = projectDirectory;
    }

    List<SourceFile> readJavaSources(String relativeDirectory) {
        Path directory = projectDirectory.resolve(relativeDirectory);
        if (!Files.isDirectory(directory)) {
            throw new IllegalStateException("Missing source directory " + directory);
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .map(path -> read(directory, path))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list " + directory, e);
        }
    }

    private static SourceFile read(Path directory, Path file) {
        String relative = directory.relativize(file).toString().replace('\\', '/');
        try {
            return new SourceFile(relative, Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }
}
