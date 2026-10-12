package tlm.dev.lectern.diff.model;

import java.util.Objects;
import java.util.Optional;

public record CommandRedirectModified(CommandPath path, Optional<CommandPath> oldRedirect, Optional<CommandPath> newRedirect)
        implements CommandChange {

    public CommandRedirectModified {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(oldRedirect, "oldRedirect");
        Objects.requireNonNull(newRedirect, "newRedirect");
        if (oldRedirect.equals(newRedirect)) {
            throw new IllegalArgumentException("The redirect of " + path + " did not change");
        }
    }
}
