package tlm.dev.lectern.diff.model;

import java.util.Objects;
import java.util.Optional;

public record CommandPermissionsModified(
        CommandPath path, Optional<PermissionRule> oldPermissions, Optional<PermissionRule> newPermissions)
        implements CommandChange {

    public CommandPermissionsModified {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(oldPermissions, "oldPermissions");
        Objects.requireNonNull(newPermissions, "newPermissions");
        if (oldPermissions.equals(newPermissions)) {
            throw new IllegalArgumentException("The permissions of " + path + " did not change");
        }
    }
}
