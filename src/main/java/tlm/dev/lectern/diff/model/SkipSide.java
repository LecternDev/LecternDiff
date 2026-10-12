package tlm.dev.lectern.diff.model;

import java.util.List;

public sealed interface SkipSide permits SkipSide.From, SkipSide.To, SkipSide.Both {

    SkipSide FROM = new From();
    SkipSide TO = new To();
    SkipSide BOTH = new Both();
    List<SkipSide> ALL = List.of(FROM, TO, BOTH);

    String label();

    record From() implements SkipSide {

        @Override
        public String label() {
            return "from";
        }
    }

    record To() implements SkipSide {

        @Override
        public String label() {
            return "to";
        }
    }

    record Both() implements SkipSide {

        @Override
        public String label() {
            return "both";
        }
    }
}
