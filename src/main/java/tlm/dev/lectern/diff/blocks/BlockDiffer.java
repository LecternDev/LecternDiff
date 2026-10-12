package tlm.dev.lectern.diff.blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import tlm.dev.lectern.diff.ids.IdShiftClassifier;
import tlm.dev.lectern.diff.model.Block;
import tlm.dev.lectern.diff.model.BlockAdded;
import tlm.dev.lectern.diff.model.BlockChange;
import tlm.dev.lectern.diff.model.BlockDefaultStateModified;
import tlm.dev.lectern.diff.model.BlockKey;
import tlm.dev.lectern.diff.model.BlockPropertyAdded;
import tlm.dev.lectern.diff.model.BlockPropertyRemoved;
import tlm.dev.lectern.diff.model.BlockPropertyValuesAdded;
import tlm.dev.lectern.diff.model.BlockPropertyValuesRemoved;
import tlm.dev.lectern.diff.model.BlockRemoved;
import tlm.dev.lectern.diff.model.BlockStateIdsModified;
import tlm.dev.lectern.diff.model.BlockStateIdsShifted;
import tlm.dev.lectern.diff.model.Blocks;
import tlm.dev.lectern.diff.model.PropertyName;
import tlm.dev.lectern.diff.model.PropertyValue;
import tlm.dev.lectern.diff.model.StateId;
import tlm.dev.lectern.diff.model.StateProperties;

public final class BlockDiffer {

    private record StateKey(BlockKey block, StateProperties state) implements Comparable<StateKey> {

        @Override
        public int compareTo(StateKey other) {
            int byBlock = block.compareTo(other.block);
            return byBlock != 0 ? byBlock : state.compareTo(other.state);
        }
    }

    private final IdShiftClassifier classifier;

    public BlockDiffer(IdShiftClassifier classifier) {
        this.classifier = classifier;
    }

    public List<BlockChange> diff(Blocks from, Blocks to) {
        List<BlockChange> changes = new ArrayList<>();
        for (Block block : from.byKey().values()) {
            if (!to.byKey().containsKey(block.key())) {
                changes.add(new BlockRemoved(block.key(), block.states().size()));
            }
        }
        for (Block block : to.byKey().values()) {
            Block before = from.byKey().get(block.key());
            if (before == null) {
                changes.add(new BlockAdded(block.key(), block.states().size()));
            } else {
                diffBlock(before, block, changes);
            }
        }
        diffStateIds(from, to, changes);
        return List.copyOf(changes);
    }

    private void diffBlock(Block before, Block after, List<BlockChange> changes) {
        Set<PropertyName> names = new TreeSet<>(before.properties().keySet());
        names.addAll(after.properties().keySet());
        for (PropertyName name : names) {
            List<PropertyValue> was = before.properties().get(name);
            List<PropertyValue> now = after.properties().get(name);
            if (was == null) {
                changes.add(new BlockPropertyAdded(before.key(), name, now, after.defaultState().values().get(name)));
            } else if (now == null) {
                changes.add(new BlockPropertyRemoved(before.key(), name, was, before.defaultState().values().get(name)));
            } else {
                diffValues(before.key(), name, was, now, changes);
            }
        }
        StateProperties oldDefault = restrictedTo(before.defaultState(), before, after);
        StateProperties newDefault = restrictedTo(after.defaultState(), before, after);
        if (!oldDefault.equals(newDefault)) {
            changes.add(new BlockDefaultStateModified(before.key(), oldDefault, newDefault));
        }
    }

    private static StateProperties restrictedTo(StateProperties state, Block before, Block after) {
        Map<PropertyName, PropertyValue> kept = new TreeMap<>();
        state.values().forEach((name, value) -> {
            if (before.properties().containsKey(name) && after.properties().containsKey(name)) {
                kept.put(name, value);
            }
        });
        return new StateProperties(kept);
    }

    private void diffValues(
            BlockKey block, PropertyName name, List<PropertyValue> was, List<PropertyValue> now, List<BlockChange> changes) {
        List<PropertyValue> added = now.stream().filter(value -> !was.contains(value)).toList();
        List<PropertyValue> removed = was.stream().filter(value -> !now.contains(value)).toList();
        if (!added.isEmpty()) {
            changes.add(new BlockPropertyValuesAdded(block, name, added));
        }
        if (!removed.isEmpty()) {
            changes.add(new BlockPropertyValuesRemoved(block, name, removed));
        }
    }

    private void diffStateIds(Blocks from, Blocks to, List<BlockChange> changes) {
        IdShiftClassifier.Result<StateKey> ids = classifier.classify(stateIds(from), stateIds(to));
        ids.firstShiftedId().ifPresent(first -> changes.add(new BlockStateIdsShifted(
                ids.shiftedCount(), (int) ids.shifted().stream().map(StateKey::block).distinct().count(), new StateId(first))));
        Map<BlockKey, List<IdShiftClassifier.Modification<StateKey>>> byBlock = new TreeMap<>();
        for (IdShiftClassifier.Modification<StateKey> modification : ids.modified()) {
            byBlock.computeIfAbsent(modification.key().block(), block -> new ArrayList<>()).add(modification);
        }
        byBlock.forEach((block, modifications) -> changes.add(new BlockStateIdsModified(
                block,
                modifications.size(),
                new StateId(modifications.stream().mapToInt(IdShiftClassifier.Modification::oldId).min().orElseThrow()),
                new StateId(modifications.stream().mapToInt(IdShiftClassifier.Modification::newId).min().orElseThrow()))));
    }

    private static Map<StateKey, Integer> stateIds(Blocks blocks) {
        Map<StateKey, Integer> ids = new HashMap<>();
        for (Block block : blocks.byKey().values()) {
            block.states().forEach((state, id) -> ids.put(new StateKey(block.key(), state), id.value()));
        }
        return ids;
    }
}
