package tlm.dev.lectern.diff.model;

import java.util.Objects;

public record VersionId(String value) {

    public VersionId {
        Objects.requireNonNull(value, "value");
        if (value.isEmpty() || !value.equals(value.strip()) || value.chars().anyMatch(VersionId::forbidden)) {
            throw new IllegalArgumentException("Invalid version id: '" + value + "'");
        }
    }

    private static boolean forbidden(int character) {
        return character == '/' || character == '\\' || Character.isISOControl(character);
    }

    @Override
    public String toString() {
        return value;
    }
}
