package tlm.dev.lectern.diff.write;

import java.util.List;
import java.util.Map;
import tlm.dev.lectern.diff.json.JsonArray;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonString;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.BlockAdded;
import tlm.dev.lectern.diff.model.BlockChange;
import tlm.dev.lectern.diff.model.BlockDefaultStateModified;
import tlm.dev.lectern.diff.model.BlockPropertyAdded;
import tlm.dev.lectern.diff.model.BlockPropertyRemoved;
import tlm.dev.lectern.diff.model.BlockPropertyValuesAdded;
import tlm.dev.lectern.diff.model.BlockPropertyValuesRemoved;
import tlm.dev.lectern.diff.model.BlockRemoved;
import tlm.dev.lectern.diff.model.BlockStateIdsModified;
import tlm.dev.lectern.diff.model.BlockStateIdsShifted;
import tlm.dev.lectern.diff.model.PropertyName;
import tlm.dev.lectern.diff.model.PropertyValue;
import tlm.dev.lectern.diff.model.StateProperties;

public final class BlockChangeRenderer {

    public JsonObject render(BlockChange change) {
        return switch (change) {
            case BlockAdded added -> JsonObject.builder()
                    .put("type", "block.added")
                    .put("block", added.block().value())
                    .put("state_count", added.stateCount())
                    .build();
            case BlockRemoved removed -> JsonObject.builder()
                    .put("type", "block.removed")
                    .put("block", removed.block().value())
                    .put("state_count", removed.stateCount())
                    .build();
            case BlockPropertyAdded added -> JsonObject.builder()
                    .put("type", "block.property_added")
                    .put("block", added.block().value())
                    .put("property", added.property().value())
                    .put("values", values(added.values()))
                    .build();
            case BlockPropertyRemoved removed -> JsonObject.builder()
                    .put("type", "block.property_removed")
                    .put("block", removed.block().value())
                    .put("property", removed.property().value())
                    .put("values", values(removed.values()))
                    .build();
            case BlockPropertyValuesAdded added -> JsonObject.builder()
                    .put("type", "block.property_values_added")
                    .put("block", added.block().value())
                    .put("property", added.property().value())
                    .put("values", values(added.values()))
                    .build();
            case BlockPropertyValuesRemoved removed -> JsonObject.builder()
                    .put("type", "block.property_values_removed")
                    .put("block", removed.block().value())
                    .put("property", removed.property().value())
                    .put("values", values(removed.values()))
                    .build();
            case BlockDefaultStateModified modified -> JsonObject.builder()
                    .put("type", "block.default_state_modified")
                    .put("block", modified.block().value())
                    .put("old_default", state(modified.oldDefault()))
                    .put("new_default", state(modified.newDefault()))
                    .build();
            case BlockStateIdsShifted shifted -> JsonObject.builder()
                    .put("type", "block.state_ids_shifted")
                    .put("state_count", shifted.stateCount())
                    .put("block_count", shifted.blockCount())
                    .put("first_id", shifted.firstId().value())
                    .build();
            case BlockStateIdsModified modified -> JsonObject.builder()
                    .put("type", "block.state_ids_modified")
                    .put("block", modified.block().value())
                    .put("state_count", modified.stateCount())
                    .put("old_first_id", modified.oldFirstId().value())
                    .put("new_first_id", modified.newFirstId().value())
                    .build();
        };
    }

    private static JsonValue values(List<PropertyValue> values) {
        return new JsonArray(values.stream().<JsonValue>map(value -> new JsonString(value.value())).toList());
    }

    private static JsonValue state(StateProperties state) {
        JsonObject.Builder builder = JsonObject.builder();
        for (Map.Entry<PropertyName, PropertyValue> property : state.values().entrySet()) {
            builder.put(property.getKey().value(), property.getValue().value());
        }
        return builder.build();
    }
}
