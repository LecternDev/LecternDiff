package tlm.dev.lectern.diff.model;

public record SkippedFolder(String folder, int added, int removed, int modified) implements Skipped {

    public SkippedFolder {
        SnakeCaseSyntax.require(folder, "skipped folder");
        if (added < 0 || removed < 0 || modified < 0) {
            throw new IllegalArgumentException("The file counts of " + folder + " are not negative");
        }
        if (added + removed + modified == 0) {
            throw new IllegalArgumentException("The folder " + folder + " has no changed file");
        }
    }

    @Override
    public SkipReason reason() {
        return new SkipReason.NotCompared();
    }
}
