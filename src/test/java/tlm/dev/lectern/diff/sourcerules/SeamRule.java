package tlm.dev.lectern.diff.sourcerules;

import java.util.List;

record SeamRule(String from, List<String> forbidden) {

    SeamRule {
        forbidden = List.copyOf(forbidden);
        if (forbidden.isEmpty()) {
            throw new IllegalArgumentException("Seam from " + from + " forbids nothing");
        }
    }
}
