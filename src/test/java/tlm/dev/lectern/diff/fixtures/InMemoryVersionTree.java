package tlm.dev.lectern.diff.fixtures;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import tlm.dev.lectern.diff.model.VersionRef;
import tlm.dev.lectern.diff.read.VersionTree;

public final class InMemoryVersionTree implements VersionTree {

    private final VersionRef version;
    private final Map<String, String> files;

    public InMemoryVersionTree(VersionRef version, Map<String, String> files) {
        this.version = version;
        this.files = Map.copyOf(files);
    }

    @Override
    public VersionRef version() {
        return version;
    }

    @Override
    public Optional<String> readText(String relativePath) {
        return Optional.ofNullable(files.get(relativePath));
    }

    @Override
    public List<String> files(String directory) {
        return files.keySet().stream().filter(path -> path.startsWith(directory + "/")).sorted().toList();
    }
}
