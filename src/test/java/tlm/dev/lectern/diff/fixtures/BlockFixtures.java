package tlm.dev.lectern.diff.fixtures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tlm.dev.lectern.diff.model.Block;
import tlm.dev.lectern.diff.model.BlockKey;
import tlm.dev.lectern.diff.model.Blocks;
import tlm.dev.lectern.diff.model.PropertyName;
import tlm.dev.lectern.diff.model.PropertyValue;
import tlm.dev.lectern.diff.model.StateId;
import tlm.dev.lectern.diff.model.StateProperties;

public final class BlockFixtures {

    private BlockFixtures() {
    }

    public static BlockBuilder block(String key) {
        return new BlockBuilder(key);
    }

    public static Block plain(String key, int stateId) {
        return block(key).state(stateId, "").build();
    }

    public static Block single(String key, int firstStateId, String property, String... values) {
        BlockBuilder builder = block(key).property(property, values);
        for (int index = 0; index < values.length; index++) {
            builder.state(firstStateId + index, property + "=" + values[index]);
        }
        return builder.build();
    }

    public static Blocks blocks(Block... blocks) {
        Map<BlockKey, Block> byKey = new TreeMap<>();
        for (Block block : blocks) {
            byKey.put(block.key(), block);
        }
        return new Blocks(byKey);
    }

    public static BlockKey blockKey(String key) {
        return new BlockKey(key);
    }

    public static PropertyName property(String name) {
        return new PropertyName(name);
    }

    public static PropertyValue value(String value) {
        return new PropertyValue(value);
    }

    public static StateId stateId(int id) {
        return new StateId(id);
    }

    public static List<PropertyValue> values(String... values) {
        return List.of(values).stream().map(PropertyValue::new).toList();
    }

    public static StateProperties state(String assignment) {
        Map<PropertyName, PropertyValue> values = new TreeMap<>();
        if (!assignment.isEmpty()) {
            for (String pair : assignment.split(",")) {
                String[] parts = pair.split("=");
                values.put(new PropertyName(parts[0]), new PropertyValue(parts[1]));
            }
        }
        return new StateProperties(values);
    }

    public static final class BlockBuilder {

        private final BlockKey key;
        private final Map<PropertyName, List<PropertyValue>> properties = new TreeMap<>();
        private final Map<StateProperties, StateId> states = new TreeMap<>();
        private final List<StateProperties> order = new ArrayList<>();
        private StateProperties defaultState;

        private BlockBuilder(String key) {
            this.key = new BlockKey(key);
        }

        public BlockBuilder property(String name, String... values) {
            properties.put(new PropertyName(name), values(values));
            return this;
        }

        public BlockBuilder state(int id, String assignment) {
            StateProperties state = BlockFixtures.state(assignment);
            states.put(state, new StateId(id));
            order.add(state);
            return this;
        }

        public BlockBuilder defaultState(String assignment) {
            this.defaultState = BlockFixtures.state(assignment);
            return this;
        }

        public Block build() {
            return new Block(key, properties, states, defaultState == null ? order.get(0) : defaultState);
        }
    }
}
