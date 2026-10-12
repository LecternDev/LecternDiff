package tlm.dev.lectern.diff;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tlm.dev.lectern.diff.model.VersionId;
import tlm.dev.lectern.diff.model.VersionKind;
import tlm.dev.lectern.diff.model.VersionRef;

record CommandLine(Path from, Path to, VersionRef fromVersion, VersionRef toVersion) {

    static final String USAGE =
            "usage: diff <version-folder-A> <version-folder-B> --from <id> --from-kind <kind> --to <id> --to-kind <kind>";

    private static final String COMMAND = "diff";
    private static final String FROM = "--from";
    private static final String FROM_KIND = "--from-kind";
    private static final String TO = "--to";
    private static final String TO_KIND = "--to-kind";
    private static final List<String> OPTIONS = List.of(FROM, FROM_KIND, TO, TO_KIND);
    private static final int FOLDER_COUNT = 2;

    static CommandLine parse(String[] args) {
        if (args.length == 0 || !COMMAND.equals(args[0])) {
            throw new UsageException("the first argument must be '" + COMMAND + "'");
        }
        List<String> folders = new ArrayList<>();
        Map<String, String> options = new TreeMap<>();
        for (int index = 1; index < args.length; index++) {
            String argument = args[index];
            if (!argument.startsWith("--")) {
                folders.add(argument);
                continue;
            }
            if (!OPTIONS.contains(argument)) {
                throw new UsageException("unknown option " + argument);
            }
            if (index + 1 >= args.length || args[index + 1].startsWith("--")) {
                throw new UsageException("option " + argument + " needs a value");
            }
            if (options.putIfAbsent(argument, args[++index]) != null) {
                throw new UsageException("option " + argument + " is given twice");
            }
        }
        if (folders.size() != FOLDER_COUNT) {
            throw new UsageException("expected " + FOLDER_COUNT + " version folders, got " + folders.size());
        }
        for (String option : OPTIONS) {
            if (!options.containsKey(option)) {
                throw new UsageException("missing option " + option);
            }
        }
        return new CommandLine(
                path(folders.get(0)),
                path(folders.get(1)),
                version(options.get(FROM), options.get(FROM_KIND)),
                version(options.get(TO), options.get(TO_KIND)));
    }

    private static Path path(String text) {
        try {
            return Path.of(text);
        } catch (InvalidPathException e) {
            throw new UsageException("not a usable folder path: " + text, e);
        }
    }

    private static VersionRef version(String id, String kindLabel) {
        VersionKind kind = VersionKind.ofLabel(kindLabel).orElseThrow(() -> new UsageException(
                "unknown version kind '" + kindLabel + "', expected one of " + knownKinds()));
        try {
            return new VersionRef(new VersionId(id), kind);
        } catch (IllegalArgumentException e) {
            throw new UsageException("invalid version id '" + id + "'", e);
        }
    }

    private static String knownKinds() {
        return String.join(", ", VersionKind.ALL.stream().map(VersionKind::label).toList());
    }
}
