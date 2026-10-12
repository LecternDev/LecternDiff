package tlm.dev.lectern.diff.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public record Block(
        BlockKey key,
        Map<PropertyName, List<PropertyValue>> properties,
        Map<StateProperties, StateId> states,
        StateProperties defaultState) {

    public Block {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(defaultState, "defaultState");
        Map<PropertyName, List<PropertyValue>> declared = new TreeMap<>();
        properties.forEach((name, values) -> declared.put(name, List.copyOf(values)));
        properties = Collections.unmodifiableMap(declared);
        states = Collections.unmodifiableMap(new TreeMap<>(states));
        requireDeclaredValues(key, properties);
        if (states.isEmpty()) {
            throw new IllegalArgumentException("Block " + key + " has no state");
        }
        if (!states.containsKey(defaultState)) {
            throw new IllegalArgumentException("The default state " + defaultState + " is not a state of block " + key);
        }
        Set<StateId> seen = new HashSet<>();
        for (Map.Entry<StateProperties, StateId> state : states.entrySet()) {
            requireDeclaredAssignment(key, properties, state.getKey());
            if (!seen.add(state.getValue())) {
                throw new IllegalArgumentException("State id " + state.getValue() + " is used twice in block " + key);
            }
        }
    }

    private static void requireDeclaredValues(BlockKey key, Map<PropertyName, List<PropertyValue>> properties) {
        for (Map.Entry<PropertyName, List<PropertyValue>> property : properties.entrySet()) {
            if (property.getValue().isEmpty()) {
                throw new IllegalArgumentException("Property " + property.getKey() + " of block " + key + " has no value");
            }
            if (Set.copyOf(property.getValue()).size() != property.getValue().size()) {
                throw new IllegalArgumentException(
                        "Property " + property.getKey() + " of block " + key + " lists a value twice");
            }
        }
    }

    private static void requireDeclaredAssignment(
            BlockKey key, Map<PropertyName, List<PropertyValue>> properties, StateProperties state) {
        if (!state.values().keySet().equals(properties.keySet())) {
            throw new IllegalArgumentException("A state of block " + key + " sets " + state.values().keySet()
                    + " but the block declares " + properties.keySet());
        }
        for (Map.Entry<PropertyName, PropertyValue> assigned : state.values().entrySet()) {
            if (!properties.get(assigned.getKey()).contains(assigned.getValue())) {
                throw new IllegalArgumentException("A state of block " + key + " sets " + assigned.getKey() + " to "
                        + assigned.getValue() + ", which the block does not declare");
            }
        }
    }
}
