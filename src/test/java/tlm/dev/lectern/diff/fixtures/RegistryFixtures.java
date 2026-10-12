package tlm.dev.lectern.diff.fixtures;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import tlm.dev.lectern.diff.model.EntryKey;
import tlm.dev.lectern.diff.model.ProtocolId;
import tlm.dev.lectern.diff.model.Registries;
import tlm.dev.lectern.diff.model.Registry;
import tlm.dev.lectern.diff.model.RegistryKey;

public final class RegistryFixtures {

    private RegistryFixtures() {
    }

    public static RegistryBuilder registry(String key, int id) {
        return new RegistryBuilder(key, id);
    }

    public static Registries registries(Registry... registries) {
        Map<RegistryKey, Registry> byKey = new TreeMap<>();
        for (Registry registry : registries) {
            byKey.put(registry.key(), registry);
        }
        return new Registries(byKey);
    }

    public static EntryKey entry(String key) {
        return new EntryKey(key);
    }

    public static RegistryKey key(String key) {
        return new RegistryKey(key);
    }

    public static ProtocolId id(int value) {
        return new ProtocolId(value);
    }

    public static final class RegistryBuilder {

        private final String key;
        private final int id;
        private final Map<EntryKey, ProtocolId> entries = new TreeMap<>();
        private Optional<EntryKey> defaultEntry = Optional.empty();

        private RegistryBuilder(String key, int id) {
            this.key = key;
            this.id = id;
        }

        public RegistryBuilder defaultEntry(String entry) {
            this.defaultEntry = Optional.of(new EntryKey(entry));
            return this;
        }

        public RegistryBuilder entry(String entry, int entryId) {
            entries.put(new EntryKey(entry), new ProtocolId(entryId));
            return this;
        }

        public Registry build() {
            return new Registry(new RegistryKey(key), new ProtocolId(id), defaultEntry, entries);
        }
    }
}
