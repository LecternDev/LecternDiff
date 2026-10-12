package tlm.dev.lectern.diff.read;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.EntryKey;
import tlm.dev.lectern.diff.model.ProtocolId;
import tlm.dev.lectern.diff.model.Registries;
import tlm.dev.lectern.diff.model.Registry;
import tlm.dev.lectern.diff.model.RegistryKey;

public final class RegistryReportReader {

    public static final String FILE = "reports/registries.json";

    private static final String DEFAULT = "default";
    private static final String ENTRIES = "entries";
    private static final String PROTOCOL_ID = "protocol_id";
    private static final Set<String> REGISTRY_KEYS = Set.of(DEFAULT, ENTRIES, PROTOCOL_ID);
    private static final Set<String> ENTRY_KEYS = Set.of(PROTOCOL_ID);

    private final JsonReader json;

    public RegistryReportReader(JsonReader json) {
        this.json = json;
    }

    public Registries read(VersionTree tree) {
        ReportContext report = new ReportContext(tree.version().id(), FILE);
        JsonValue root = report.parse(tree, json);
        try {
            return new Registries(readRegistries(report, root));
        } catch (IllegalArgumentException e) {
            throw report.failure(e.getMessage(), e);
        }
    }

    private Map<RegistryKey, Registry> readRegistries(ReportContext report, JsonValue root) {
        JsonObject top = report.object(root, "the document");
        Map<RegistryKey, Registry> byKey = new TreeMap<>();
        for (Map.Entry<String, JsonValue> member : top.members().entrySet()) {
            RegistryKey key = new RegistryKey(member.getKey());
            byKey.put(key, readRegistry(report, key, report.object(member.getValue(), "registry " + key)));
        }
        return byKey;
    }

    private Registry readRegistry(ReportContext report, RegistryKey key, JsonObject registry) {
        String where = "registry " + key;
        report.rejectUnknownKeys(registry, REGISTRY_KEYS, where);
        ProtocolId id = new ProtocolId(report.nonNegativeInt(registry, PROTOCOL_ID, where));
        JsonObject entries = report.object(report.member(registry, ENTRIES, where), "entries of registry " + key);
        Map<EntryKey, ProtocolId> read = new TreeMap<>();
        for (Map.Entry<String, JsonValue> member : entries.members().entrySet()) {
            String entryWhere = "entry " + member.getKey() + " of registry " + key;
            JsonObject entry = report.object(member.getValue(), entryWhere);
            report.rejectUnknownKeys(entry, ENTRY_KEYS, entryWhere);
            read.put(new EntryKey(member.getKey()), new ProtocolId(report.nonNegativeInt(entry, PROTOCOL_ID, entryWhere)));
        }
        Optional<EntryKey> defaultEntry = registry.member(DEFAULT)
                .map(value -> new EntryKey(report.string(value, "default of registry " + key)));
        return new Registry(key, id, defaultEntry, read);
    }
}
