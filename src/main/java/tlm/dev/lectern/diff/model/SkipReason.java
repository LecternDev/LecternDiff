package tlm.dev.lectern.diff.model;

public sealed interface SkipReason permits SkipReason.ReportMissing {

    String label();

    record ReportMissing() implements SkipReason {

        @Override
        public String label() {
            return "report_missing";
        }
    }
}
