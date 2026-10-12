package tlm.dev.lectern.diff.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public record Registry(RegistryKey key, ProtocolId id, Optional<EntryKey> defaultEntry, Map<EntryKey, ProtocolId> entries) {

    public Registry {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(defaultEntry, "defaultEntry");
        entries = Collections.unmodifiableMap(new TreeMap<>(entries));
        if (defaultEntry.isPresent() && !entries.containsKey(defaultEntry.get())) {
            throw new IllegalArgumentException(
                    "Default entry " + defaultEntry.get() + " is not an entry of registry " + key);
        }
        Set<ProtocolId> seen = new HashSet<>();
        for (Map.Entry<EntryKey, ProtocolId> entry : entries.entrySet()) {
            if (!seen.add(entry.getValue())) {
                throw new IllegalArgumentException(
                        "Protocol id " + entry.getValue() + " is used twice in registry " + key);
            }
        }
    }
}
