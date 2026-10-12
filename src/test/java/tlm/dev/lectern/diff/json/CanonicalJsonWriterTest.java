package tlm.dev.lectern.diff.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

class CanonicalJsonWriterTest {

    private static final String CANONICAL = """
            {
              "alpha": [
                1,
                0.0,
                {
                  "inner": null,
                  "z": true
                }
              ],
              "beta": {},
              "gamma": [],
              "omega": "text"
            }
            """;

    private final CanonicalJsonWriter writer = new CanonicalJsonWriter();
    private final JsonReader reader = new JsonReader();

    @Test
    void keysAreSortedIndentedByTwoSpacesAndTheDocumentEndsWithOneNewline() {
        JsonObject inner = JsonObject.builder().put("z", new JsonBoolean(true)).putNull("inner").build();
        JsonObject root = JsonObject.builder()
                .put("omega", "text")
                .put("gamma", new JsonArray(List.of()))
                .put("beta", JsonObject.builder().build())
                .put("alpha", new JsonArray(List.of(JsonNumber.of(1), new JsonNumber("0.0"), inner)))
                .build();
        assertEquals(CANONICAL, writer.write(root));
    }

    @Test
    void rewritingACanonicalDocumentGivesTheSameBytes() {
        assertEquals(CANONICAL, writer.write(reader.read(CANONICAL)));
    }

    @Test
    void aDocumentWrittenInAnotherLayoutBecomesCanonical() {
        String untidy = "{\"omega\":\"text\",  \"gamma\":[ ],\"beta\":{ },\n\"alpha\":[1,0.0,{\"z\":true,\"inner\":null}]}";
        assertEquals(CANONICAL, writer.write(reader.read(untidy)));
    }

    @Test
    void twoWritesOfTheSameDocumentGiveTheSameBytes() {
        JsonValue document = reader.read(CANONICAL);
        assertEquals(writer.write(document), writer.write(document));
    }

    @Test
    void numbersAreWrittenAsTheyWereRead() {
        JsonValue document = reader.read("[0.0, -0.5, 1E3, 2.50]");
        assertEquals("[\n  0.0,\n  -0.5,\n  1E3,\n  2.50\n]\n", writer.write(document));
    }

    @Test
    void outputUsesLineFeedsOnly() {
        assertFalse(writer.write(reader.read(CANONICAL)).contains("\r"));
    }

    @Test
    void quotesBackslashesAndControlCharactersAreEscaped() {
        String control = String.valueOf((char) 1);
        String written = writer.write(new JsonString("a\"b\\c\n" + control));
        String escapedControl = "\\" + "u0001";
        assertEquals("\"a\\\"b\\\\c\\n" + escapedControl + "\"\n", written);
    }

    @Test
    void nonAsciiCharactersAreWrittenAsTheyAre() {
        String accented = "caf" + (char) 233;
        assertEquals("\"" + accented + "\"\n", writer.write(new JsonString(accented)));
    }

    @Test
    void escapedTextReadBackIsTheSameText() {
        String original = "a\"b\\c\n\t" + (char) 1 + "caf" + (char) 233;
        assertEquals(new JsonString(original), reader.read(writer.write(new JsonString(original))));
    }
}
