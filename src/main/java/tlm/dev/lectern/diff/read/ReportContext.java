package tlm.dev.lectern.diff.read;

import java.util.Optional;
import java.util.Set;
import tlm.dev.lectern.diff.json.JsonArray;
import tlm.dev.lectern.diff.json.JsonBoolean;
import tlm.dev.lectern.diff.json.JsonNumber;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.json.JsonString;
import tlm.dev.lectern.diff.json.JsonSyntaxException;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.VersionId;

final class ReportContext {

    private final VersionId version;
    private final String file;

    ReportContext(VersionId version, String file) {
        this.version = version;
        this.file = file;
    }

    Optional<JsonValue> parse(VersionTree tree, JsonReader json) {
        Optional<String> text = tree.readText(file);
        if (text.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(json.read(text.get()));
        } catch (JsonSyntaxException e) {
            throw failure("not valid JSON: " + e.getMessage(), e);
        }
    }

    JsonObject object(JsonValue value, String what) {
        if (value instanceof JsonObject object) {
            return object;
        }
        throw failure(what + " is not an object");
    }

    JsonArray array(JsonValue value, String what) {
        if (value instanceof JsonArray array) {
            return array;
        }
        throw failure(what + " is not an array");
    }

    String string(JsonValue value, String what) {
        if (value instanceof JsonString string) {
            return string.value();
        }
        throw failure(what + " is not a string");
    }

    void requireTrue(JsonValue value, String what) {
        if (!(value instanceof JsonBoolean flag) || !flag.value()) {
            throw failure(what + " is not true");
        }
    }

    JsonValue member(JsonObject object, String name, String where) {
        return object.member(name).orElseThrow(() -> failure("missing key '" + name + "' in " + where));
    }

    void rejectUnknownKeys(JsonObject object, Set<String> known, String where) {
        for (String name : object.members().keySet()) {
            if (!known.contains(name)) {
                throw failure("unknown key '" + name + "' in " + where);
            }
        }
    }

    int nonNegativeInt(JsonObject object, String name, String where) {
        JsonValue value = member(object, name, where);
        if (!(value instanceof JsonNumber number)) {
            throw failure(name + " of " + where + " is not a number");
        }
        int parsed;
        try {
            parsed = Integer.parseInt(number.text());
        } catch (NumberFormatException e) {
            throw failure(notAnInteger(name, where, number), e);
        }
        if (parsed < 0 || !Integer.toString(parsed).equals(number.text())) {
            throw failure(notAnInteger(name, where, number));
        }
        return parsed;
    }

    VersionReadException failure(String detail) {
        return new VersionReadException("Version " + version + ", " + file + ": " + detail);
    }

    VersionReadException failure(String detail, Throwable cause) {
        return new VersionReadException("Version " + version + ", " + file + ": " + detail, cause);
    }

    private static String notAnInteger(String name, String where, JsonNumber number) {
        return name + " of " + where + " is not a non-negative integer: " + number.text();
    }
}
