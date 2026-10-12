package tlm.dev.lectern.diff.model;

public record ProtocolId(int value) {

    public ProtocolId {
        if (value < 0) {
            throw new IllegalArgumentException("A protocol id cannot be negative: " + value);
        }
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
