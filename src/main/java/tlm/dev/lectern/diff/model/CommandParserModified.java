package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record CommandParserModified(CommandPath path, ParserId oldParser, ParserId newParser) implements CommandChange {

    public CommandParserModified {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(oldParser, "oldParser");
        Objects.requireNonNull(newParser, "newParser");
        if (oldParser.equals(newParser)) {
            throw new IllegalArgumentException("The parser of " + path + " did not change");
        }
    }
}
