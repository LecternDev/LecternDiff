package tlm.dev.lectern.diff.fixtures;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class VersionFolders {

    private VersionFolders() {
    }

    public static Path write(
            Path workDirectory, String name, String registriesJson, String blocksJson, String packetsJson, String commandsJson)
            throws IOException {
        return write(workDirectory, name, Map.of("reports/registries.json", registriesJson, "reports/blocks.json", blocksJson,
                "reports/packets.json", packetsJson, "reports/commands.json", commandsJson));
    }

    public static Path write(Path workDirectory, String name, Map<String, String> files) throws IOException {
        Path folder = Files.createDirectories(workDirectory.resolve(name));
        for (Map.Entry<String, String> file : files.entrySet()) {
            Path target = folder.resolve(file.getKey());
            Files.createDirectories(target.getParent());
            Files.writeString(target, file.getValue(), StandardCharsets.UTF_8);
        }
        return folder;
    }

    public static Path before(Path workDirectory) throws IOException {
        return write(workDirectory, "before", GadgetScenario.beforeFiles());
    }

    public static Path after(Path workDirectory) throws IOException {
        return write(workDirectory, "after", GadgetScenario.afterFiles());
    }
}
