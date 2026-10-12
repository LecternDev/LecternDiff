package tlm.dev.lectern.diff.model;

import java.util.Objects;
import java.util.Optional;

public record CommandPropertiesModified(
        CommandPath path, Optional<ParserProperties> oldProperties, Optional<ParserProperties> newProperties)
        implements CommandChange {

    public CommandPropertiesModified {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(oldProperties, "oldProperties");
        Objects.requireNonNull(newProperties, "newProperties");
        if (oldProperties.equals(newProperties)) {
            throw new IllegalArgumentException("The properties of " + path + " did not change");
        }
    }
}
