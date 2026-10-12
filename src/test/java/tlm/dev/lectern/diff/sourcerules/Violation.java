package tlm.dev.lectern.diff.sourcerules;

record Violation(String path, int line, String message) {

    String describe() {
        return path + ":" + line + " " + message;
    }
}
