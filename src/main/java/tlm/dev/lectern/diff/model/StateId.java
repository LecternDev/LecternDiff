package tlm.dev.lectern.diff.model;

public record StateId(int value) {

    public StateId {
        if (value < 0) {
            throw new IllegalArgumentException("A state id cannot be negative: " + value);
        }
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
