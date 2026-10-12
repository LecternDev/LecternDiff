package tlm.dev.lectern.diff;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import tlm.dev.lectern.diff.json.CanonicalJsonWriter;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.read.DirectoryVersionTree;
import tlm.dev.lectern.diff.read.RegistryReportReader;
import tlm.dev.lectern.diff.read.VersionReadException;
import tlm.dev.lectern.diff.read.VersionTree;
import tlm.dev.lectern.diff.registries.IdShiftClassifier;
import tlm.dev.lectern.diff.registries.RegistryDiffer;
import tlm.dev.lectern.diff.write.ChangeSetWriter;
import tlm.dev.lectern.diff.write.RegistryChangeRenderer;

public final class Main {

    static final int EXIT_OK = 0;
    static final int EXIT_FAILURE = 1;
    static final int EXIT_USAGE = 2;

    private final VersionDiffService service;
    private final ChangeSetWriter writer;

    Main(VersionDiffService service, ChangeSetWriter writer) {
        this.service = service;
        this.writer = writer;
    }

    public static void main(String[] args) {
        System.exit(create().run(args, System.out, System.err));
    }

    static Main create() {
        JsonReader jsonReader = new JsonReader();
        VersionDiffService service = new VersionDiffService(
                new RegistryReportReader(jsonReader), new RegistryDiffer(new IdShiftClassifier()));
        ChangeSetWriter writer = new ChangeSetWriter(new RegistryChangeRenderer(), new CanonicalJsonWriter());
        return new Main(service, writer);
    }

    int run(String[] args, PrintStream out, PrintStream err) {
        try {
            CommandLine command = CommandLine.parse(args);
            VersionTree from = new DirectoryVersionTree(command.from(), command.fromVersion());
            VersionTree to = new DirectoryVersionTree(command.to(), command.toVersion());
            out.writeBytes(writer.write(service.diff(from, to)).getBytes(StandardCharsets.UTF_8));
            out.flush();
            if (out.checkError()) {
                return fail(err, "error: cannot write the change set to the standard output", EXIT_FAILURE);
            }
            return EXIT_OK;
        } catch (UsageException e) {
            return fail(err, "error: " + e.getMessage() + "\n" + CommandLine.USAGE, EXIT_USAGE);
        } catch (VersionReadException e) {
            return fail(err, "error: " + e.getMessage(), EXIT_FAILURE);
        }
    }

    private static int fail(PrintStream err, String message, int exitCode) {
        err.writeBytes((message + "\n").getBytes(StandardCharsets.UTF_8));
        err.flush();
        return exitCode;
    }
}
