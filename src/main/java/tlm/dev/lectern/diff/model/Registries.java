package tlm.dev.lectern.diff.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public record Registries(Map<RegistryKey, Registry> byKey) {

    public Registries {
        byKey = Collections.unmodifiableMap(new TreeMap<>(byKey));
        Set<ProtocolId> seen = new HashSet<>();
        for (Map.Entry<RegistryKey, Registry> entry : byKey.entrySet()) {
            if (!entry.getKey().equals(entry.getValue().key())) {
                throw new IllegalArgumentException(
                        "Registry " + entry.getValue().key() + " is stored under " + entry.getKey());
            }
            if (!seen.add(entry.getValue().id())) {
                throw new IllegalArgumentException(
                        "Protocol id " + entry.getValue().id() + " is used by two registries");
            }
        }
    }

    public Map<RegistryKey, ProtocolId> ids() {
        Map<RegistryKey, ProtocolId> ids = new TreeMap<>();
        byKey.forEach((key, registry) -> ids.put(key, registry.id()));
        return Collections.unmodifiableMap(ids);
    }
}
