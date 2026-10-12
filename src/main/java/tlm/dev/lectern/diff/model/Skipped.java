package tlm.dev.lectern.diff.model;

public sealed interface Skipped permits SkippedDomain, SkippedFolder {

    SkipReason reason();
}
