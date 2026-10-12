package tlm.dev.lectern.diff.model;

import java.util.List;
import java.util.Optional;

public sealed interface NodeKind permits NodeKind.Literal, NodeKind.Argument {

    NodeKind LITERAL = new Literal();
    NodeKind ARGUMENT = new Argument();
    List<NodeKind> ALL = List.of(LITERAL, ARGUMENT);

    String label();

    static Optional<NodeKind> ofLabel(String label) {
        return ALL.stream().filter(kind -> kind.label().equals(label)).findFirst();
    }

    record Literal() implements NodeKind {

        @Override
        public String label() {
            return "literal";
        }
    }

    record Argument() implements NodeKind {

        @Override
        public String label() {
            return "argument";
        }
    }
}
