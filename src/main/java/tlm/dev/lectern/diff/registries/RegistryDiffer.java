package tlm.dev.lectern.diff.registries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tlm.dev.lectern.diff.ids.IdShiftClassifier;
import tlm.dev.lectern.diff.model.EntryKey;
import tlm.dev.lectern.diff.model.ProtocolId;
import tlm.dev.lectern.diff.model.Registries;
import tlm.dev.lectern.diff.model.Registry;
import tlm.dev.lectern.diff.model.RegistryAdded;
import tlm.dev.lectern.diff.model.RegistryChange;
import tlm.dev.lectern.diff.model.RegistryDefaultModified;
import tlm.dev.lectern.diff.model.RegistryEntryAdded;
import tlm.dev.lectern.diff.model.RegistryEntryIdModified;
import tlm.dev.lectern.diff.model.RegistryEntryIdsShifted;
import tlm.dev.lectern.diff.model.RegistryEntryRemoved;
import tlm.dev.lectern.diff.model.RegistryIdModified;
import tlm.dev.lectern.diff.model.RegistryIdsShifted;
import tlm.dev.lectern.diff.model.RegistryKey;
import tlm.dev.lectern.diff.model.RegistryRemoved;

public final class RegistryDiffer {

    private final IdShiftClassifier classifier;

    public RegistryDiffer(IdShiftClassifier classifier) {
        this.classifier = classifier;
    }

    public List<RegistryChange> diff(Registries from, Registries to) {
        List<RegistryChange> changes = new ArrayList<>();
        diffRegistryTable(from, to, changes);
        for (Map.Entry<RegistryKey, Registry> before : from.byKey().entrySet()) {
            Registry after = to.byKey().get(before.getKey());
            if (after != null) {
                diffRegistry(before.getValue(), after, changes);
            }
        }
        return List.copyOf(changes);
    }

    private void diffRegistryTable(Registries from, Registries to, List<RegistryChange> changes) {
        for (Registry registry : from.byKey().values()) {
            if (!to.byKey().containsKey(registry.key())) {
                changes.add(new RegistryRemoved(registry.key(), registry.id(), registry.entries().size()));
            }
        }
        for (Registry registry : to.byKey().values()) {
            if (!from.byKey().containsKey(registry.key())) {
                changes.add(new RegistryAdded(registry.key(), registry.id(), registry.entries().size()));
            }
        }
        IdShiftClassifier.Result<RegistryKey> ids = classifier.classify(values(from.ids()), values(to.ids()));
        ids.firstShiftedId()
                .ifPresent(first -> changes.add(new RegistryIdsShifted(ids.shiftedCount(), new ProtocolId(first))));
        for (IdShiftClassifier.Modification<RegistryKey> modification : ids.modified()) {
            changes.add(new RegistryIdModified(
                    modification.key(), new ProtocolId(modification.oldId()), new ProtocolId(modification.newId())));
        }
    }

    private void diffRegistry(Registry before, Registry after, List<RegistryChange> changes) {
        RegistryKey registry = before.key();
        if (!before.defaultEntry().equals(after.defaultEntry())) {
            changes.add(new RegistryDefaultModified(registry, before.defaultEntry(), after.defaultEntry()));
        }
        for (Map.Entry<EntryKey, ProtocolId> entry : after.entries().entrySet()) {
            if (!before.entries().containsKey(entry.getKey())) {
                changes.add(new RegistryEntryAdded(registry, entry.getKey(), entry.getValue()));
            }
        }
        for (Map.Entry<EntryKey, ProtocolId> entry : before.entries().entrySet()) {
            if (!after.entries().containsKey(entry.getKey())) {
                changes.add(new RegistryEntryRemoved(registry, entry.getKey(), entry.getValue()));
            }
        }
        IdShiftClassifier.Result<EntryKey> ids =
                classifier.classify(values(before.entries()), values(after.entries()));
        ids.firstShiftedId().ifPresent(first -> changes.add(
                new RegistryEntryIdsShifted(registry, ids.shiftedCount(), new ProtocolId(first))));
        for (IdShiftClassifier.Modification<EntryKey> modification : ids.modified()) {
            changes.add(new RegistryEntryIdModified(registry, modification.key(),
                    new ProtocolId(modification.oldId()), new ProtocolId(modification.newId())));
        }
    }

    private static <K> Map<K, Integer> values(Map<K, ProtocolId> ids) {
        Map<K, Integer> values = new HashMap<>();
        ids.forEach((key, id) -> values.put(key, id.value()));
        return values;
    }
}
