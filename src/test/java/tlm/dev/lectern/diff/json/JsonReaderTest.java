package tlm.dev.lectern.diff.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class JsonReaderTest {

    private final JsonReader reader = new JsonReader();

    @Test
    void readsObjectsArraysAndScalars() {
        JsonValue value = reader.read("""
                {"list": [1, "two", true, false, null], "nested": {"k": "v"}}
                """);
        JsonObject root = (JsonObject) value;
        JsonArray list = (JsonArray) root.member("list").orElseThrow();
        assertEquals(
                List.of(new JsonNumber("1"), new JsonString("two"), new JsonBoolean(true), new JsonBoolean(false),
                        new JsonNull()),
                list.elements());
        assertEquals(new JsonString("v"), ((JsonObject) root.member("nested").orElseThrow()).member("k").orElseThrow());
    }

    @Test
    void numbersKeepTheirWrittenText() {
        JsonArray numbers = (JsonArray) reader.read("[0.0, -0.5, 1E3, 10, -0, 2.50]");
        assertEquals(
                List.of("0.0", "-0.5", "1E3", "10", "-0", "2.50"),
                numbers.elements().stream().map(element -> ((JsonNumber) element).text()).toList());
    }

    @Test
    void stringEscapesAreDecoded() {
        String text = """
                ["quote \\" backslash \\\\ newline \\n"]
                """;
        JsonArray array = (JsonArray) reader.read(text);
        assertEquals(new JsonString("quote \" backslash \\ newline \n"), array.elements().get(0));
    }

    @Test
    void memberOrderInTheSourceDoesNotMatter() {
        assertEquals(reader.read("{\"a\": 1, \"b\": 2}"), reader.read("{\"b\": 2, \"a\": 1}"));
    }

    @Test
    void duplicateMemberIsRefused() {
        JsonSyntaxException failure = assertThrows(JsonSyntaxException.class, () -> reader.read("{\"a\": 1, \"a\": 2}"));
        assertTrue(failure.getMessage().contains("Duplicate member 'a'"), failure.getMessage());
    }

    @Test
    void contentAfterTheDocumentIsRefused() {
        JsonSyntaxException failure = assertThrows(JsonSyntaxException.class, () -> reader.read("{} {}"));
        assertTrue(failure.getMessage().contains("Unexpected content after the document"), failure.getMessage());
    }

    @Test
    void emptyDocumentIsRefused() {
        for (String empty : List.of("", "   \n")) {
            JsonSyntaxException failure = assertThrows(JsonSyntaxException.class, () -> reader.read(empty));
            assertTrue(failure.getMessage().contains("empty"), failure.getMessage());
        }
    }

    @Test
    void malformedDocumentsAreRefused() {
        for (String malformed : List.of("{\"a\": }", "{\"a\": 1,}", "[1, 2", "{'a': 1}", "{/* c */}", "[NaN]", "{\"a\" 1}", "[01]")) {
            assertThrows(JsonSyntaxException.class, () -> reader.read(malformed), malformed);
        }
    }

    @Test
    void syntaxErrorsReportTheirLine() {
        JsonSyntaxException failure = assertThrows(JsonSyntaxException.class, () -> reader.read("{\n  \"a\": ]\n}"));
        assertTrue(failure.getMessage().contains("line 2"), failure.getMessage());
    }
}
