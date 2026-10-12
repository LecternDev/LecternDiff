package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record CommandExecutableModified(CommandPath path, boolean oldExecutable, boolean newExecutable)
        implements CommandChange {

    public CommandExecutableModified {
        Objects.requireNonNull(path, "path");
        if (oldExecutable == newExecutable) {
            throw new IllegalArgumentException("The executable flag of " + path + " did not change");
        }
    }
}
