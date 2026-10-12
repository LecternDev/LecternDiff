package tlm.dev.lectern.diff.fixtures;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VersionFolders {

    private VersionFolders() {
    }

    public static Path write(
            Path workDirectory, String name, String registriesJson, String blocksJson, String packetsJson, String commandsJson)
            throws IOException {
        Path reports = Files.createDirectories(workDirectory.resolve(name).resolve("reports"));
        Files.writeString(reports.resolve("registries.json"), registriesJson, StandardCharsets.UTF_8);
        Files.writeString(reports.resolve("blocks.json"), blocksJson, StandardCharsets.UTF_8);
        Files.writeString(reports.resolve("packets.json"), packetsJson, StandardCharsets.UTF_8);
        Files.writeString(reports.resolve("commands.json"), commandsJson, StandardCharsets.UTF_8);
        return workDirectory.resolve(name);
    }

    public static Path before(Path workDirectory) throws IOException {
        return write(workDirectory, "before", GadgetScenario.BEFORE, GadgetScenario.BLOCKS_BEFORE, GadgetScenario.PACKETS_BEFORE,
                GadgetScenario.COMMANDS_BEFORE);
    }

    public static Path after(Path workDirectory) throws IOException {
        return write(workDirectory, "after", GadgetScenario.AFTER, GadgetScenario.BLOCKS_AFTER, GadgetScenario.PACKETS_AFTER,
                GadgetScenario.COMMANDS_AFTER);
    }
}
