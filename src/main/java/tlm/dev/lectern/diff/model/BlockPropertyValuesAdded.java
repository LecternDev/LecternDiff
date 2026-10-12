package tlm.dev.lectern.diff.model;

import java.util.List;
import java.util.Objects;

public record BlockPropertyValuesAdded(BlockKey block, PropertyName property, List<PropertyValue> values) implements BlockChange {

    public BlockPropertyValuesAdded {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(property, "property");
        values = List.copyOf(values);
        if (values.isEmpty()) {
            throw new IllegalArgumentException("A change of property " + property + " of block " + block + " lists no value");
        }
    }
}
