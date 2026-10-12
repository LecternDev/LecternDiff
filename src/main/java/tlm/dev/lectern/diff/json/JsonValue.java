package tlm.dev.lectern.diff.json;

public sealed interface JsonValue
        permits JsonObject, JsonArray, JsonString, JsonNumber, JsonBoolean, JsonNull {
}
