package tlm.dev.lectern.diff;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

record CommandLine(Path from, Path to) {

    static final String USAGE = "usage: diff <version-folder-A> <version-folder-B>";

    private static final String COMMAND = "diff";
    private static final int ARGUMENT_COUNT = 3;

    static CommandLine parse(String[] args) {
        if (args.length != ARGUMENT_COUNT || !COMMAND.equals(args[0])) {
            throw new UsageException(USAGE);
        }
        try {
            return new CommandLine(Path.of(args[1]), Path.of(args[2]));
        } catch (InvalidPathException e) {
            throw new UsageException(USAGE + " (" + e.getMessage() + ")", e);
        }
    }
}
