package tlm.dev.lectern.diff.model;

import java.util.List;
import java.util.Objects;

public record BlockPropertyRemoved(BlockKey block, PropertyName property, List<PropertyValue> values, PropertyValue defaultValue)
        implements BlockChange {

    public BlockPropertyRemoved {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(property, "property");
        Objects.requireNonNull(defaultValue, "defaultValue");
        values = List.copyOf(values);
        if (values.isEmpty()) {
            throw new IllegalArgumentException("A change of property " + property + " of block " + block + " lists no value");
        }
        if (!values.contains(defaultValue)) {
            throw new IllegalArgumentException("The default value " + defaultValue + " of property " + property
                    + " of block " + block + " is not one of its values");
        }
    }
}
