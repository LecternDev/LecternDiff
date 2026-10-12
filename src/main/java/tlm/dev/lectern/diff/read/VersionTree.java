package tlm.dev.lectern.diff.read;

import java.util.List;
import java.util.Optional;
import tlm.dev.lectern.diff.model.VersionRef;

public interface VersionTree {

    VersionRef version();

    Optional<String> readText(String relativePath);

    List<String> files(String directory);
}
