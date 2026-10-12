package tlm.dev.lectern.diff.json;

import java.util.List;
import java.util.Map;
import tools.jackson.core.io.JsonStringEncoder;

public final class CanonicalJsonWriter {

    private static final String INDENT = "  ";

    private final JsonStringEncoder encoder = JsonStringEncoder.getInstance();

    public String write(JsonValue value) {
        StringBuilder out = new StringBuilder();
        append(out, value, 0);
        out.append('\n');
        return out.toString();
    }

    private void append(StringBuilder out, JsonValue value, int depth) {
        switch (value) {
            case JsonObject object -> appendObject(out, object, depth);
            case JsonArray array -> appendArray(out, array, depth);
            case JsonString string -> appendQuoted(out, string.value());
            case JsonNumber number -> out.append(number.text());
            case JsonBoolean bool -> out.append(bool.value());
            case JsonNull _ -> out.append("null");
        }
    }

    private void appendObject(StringBuilder out, JsonObject object, int depth) {
        if (object.members().isEmpty()) {
            out.append("{}");
            return;
        }
        out.append("{\n");
        boolean first = true;
        for (Map.Entry<String, JsonValue> member : object.members().entrySet()) {
            if (!first) {
                out.append(",\n");
            }
            first = false;
            indent(out, depth + 1);
            appendQuoted(out, member.getKey());
            out.append(": ");
            append(out, member.getValue(), depth + 1);
        }
        out.append('\n');
        indent(out, depth);
        out.append('}');
    }

    private void appendArray(StringBuilder out, JsonArray array, int depth) {
        List<JsonValue> elements = array.elements();
        if (elements.isEmpty()) {
            out.append("[]");
            return;
        }
        out.append("[\n");
        for (int index = 0; index < elements.size(); index++) {
            if (index > 0) {
                out.append(",\n");
            }
            indent(out, depth + 1);
            append(out, elements.get(index), depth + 1);
        }
        out.append('\n');
        indent(out, depth);
        out.append(']');
    }

    private void appendQuoted(StringBuilder out, String text) {
        out.append('"');
        encoder.quoteAsString(text, out);
        out.append('"');
    }

    private static void indent(StringBuilder out, int depth) {
        for (int level = 0; level < depth; level++) {
            out.append(INDENT);
        }
    }
}
