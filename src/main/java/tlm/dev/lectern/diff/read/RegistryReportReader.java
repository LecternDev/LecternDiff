package tlm.dev.lectern.diff.read;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonNumber;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.json.JsonString;
import tlm.dev.lectern.diff.json.JsonSyntaxException;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.EntryKey;
import tlm.dev.lectern.diff.model.ProtocolId;
import tlm.dev.lectern.diff.model.Registries;
import tlm.dev.lectern.diff.model.Registry;
import tlm.dev.lectern.diff.model.RegistryKey;
import tlm.dev.lectern.diff.model.VersionId;

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
        VersionId version = tree.version().id();
        String text = tree.readText(FILE).orElseThrow(() -> failure(version, "the report is missing"));
        JsonValue root;
        try {
            root = json.read(text);
        } catch (JsonSyntaxException e) {
            throw failure(version, "not valid JSON: " + e.getMessage(), e);
        }
        try {
            return new Registries(readRegistries(version, root));
        } catch (IllegalArgumentException e) {
            throw failure(version, e.getMessage(), e);
        }
    }

    private Map<RegistryKey, Registry> readRegistries(VersionId version, JsonValue root) {
        JsonObject top = requireObject(version, root, "the document");
        Map<RegistryKey, Registry> byKey = new TreeMap<>();
        for (Map.Entry<String, JsonValue> member : top.members().entrySet()) {
            RegistryKey key = new RegistryKey(member.getKey());
            byKey.put(key, readRegistry(version, key, requireObject(version, member.getValue(), "registry " + key)));
        }
        return byKey;
    }

    private Registry readRegistry(VersionId version, RegistryKey key, JsonObject registry) {
        rejectUnknownKeys(version, registry, REGISTRY_KEYS, "registry " + key);
        ProtocolId id = requireId(version, registry, "registry " + key);
        JsonObject entries = requireObject(version, require(version, registry, ENTRIES, "registry " + key),
                "entries of registry " + key);
        Map<EntryKey, ProtocolId> read = new TreeMap<>();
        for (Map.Entry<String, JsonValue> member : entries.members().entrySet()) {
            String where = "entry " + member.getKey() + " of registry " + key;
            JsonObject entry = requireObject(version, member.getValue(), where);
            rejectUnknownKeys(version, entry, ENTRY_KEYS, where);
            read.put(new EntryKey(member.getKey()), requireId(version, entry, where));
        }
        Optional<EntryKey> defaultEntry = registry.member(DEFAULT).map(value -> new EntryKey(
                requireString(version, value, "default of registry " + key)));
        return new Registry(key, id, defaultEntry, read);
    }

    private static void rejectUnknownKeys(VersionId version, JsonObject object, Set<String> known, String where) {
        for (String name : object.members().keySet()) {
            if (!known.contains(name)) {
                throw failure(version, "unknown key '" + name + "' in " + where);
            }
        }
    }

    private static JsonValue require(VersionId version, JsonObject object, String name, String where) {
        return object.member(name).orElseThrow(() -> failure(version, "missing key '" + name + "' in " + where));
    }

    private static ProtocolId requireId(VersionId version, JsonObject object, String where) {
        JsonValue value = require(version, object, PROTOCOL_ID, where);
        if (!(value instanceof JsonNumber number)) {
            throw failure(version, PROTOCOL_ID + " of " + where + " is not a number");
        }
        int parsed;
        try {
            parsed = Integer.parseInt(number.text());
        } catch (NumberFormatException e) {
            throw failure(version, notAnId(where, number), e);
        }
        if (parsed < 0 || !Integer.toString(parsed).equals(number.text())) {
            throw failure(version, notAnId(where, number));
        }
        return new ProtocolId(parsed);
    }

    private static String notAnId(String where, JsonNumber number) {
        return PROTOCOL_ID + " of " + where + " is not a non-negative integer: " + number.text();
    }

    private static JsonObject requireObject(VersionId version, JsonValue value, String what) {
        if (value instanceof JsonObject object) {
            return object;
        }
        throw failure(version, what + " is not an object");
    }

    private static String requireString(VersionId version, JsonValue value, String what) {
        if (value instanceof JsonString string) {
            return string.value();
        }
        throw failure(version, what + " is not a string");
    }

    private static VersionReadException failure(VersionId version, String detail) {
        return new VersionReadException("Version " + version + ", " + FILE + ": " + detail);
    }

    private static VersionReadException failure(VersionId version, String detail, Throwable cause) {
        return new VersionReadException("Version " + version + ", " + FILE + ": " + detail, cause);
    }
}
