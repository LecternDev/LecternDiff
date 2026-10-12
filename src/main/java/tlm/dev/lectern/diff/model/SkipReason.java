package tlm.dev.lectern.diff.model;

public sealed interface SkipReason permits SkipReason.ReportMissing, SkipReason.NotCompared {

    String label();

    record ReportMissing() implements SkipReason {

        @Override
        public String label() {
            return "report_missing";
        }
    }

    record NotCompared() implements SkipReason {

        @Override
        public String label() {
            return "not_compared";
        }
    }
}
