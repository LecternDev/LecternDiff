package tlm.dev.lectern.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SeamRulesTest {

    private static final List<String> FAMILIES = List.of(
            "model", "json", "ids", "read", "write", "registries", "blocks", "commands", "packets", "recipes", "loottables",
            "worldgen");

    private static final Map<String, Set<String>> ALLOWED = Map.ofEntries(
            Map.entry("model", Set.of()),
            Map.entry("json", Set.of()),
            Map.entry("ids", Set.of()),
            Map.entry("read", Set.of("model", "json")),
            Map.entry("write", Set.of("model", "json")),
            Map.entry("registries", Set.of("model", "ids")),
            Map.entry("blocks", Set.of("model", "ids")),
            Map.entry("commands", Set.of("model", "ids")),
            Map.entry("packets", Set.of("model", "ids")),
            Map.entry("recipes", Set.of("model", "ids")),
            Map.entry("loottables", Set.of("model", "ids")),
            Map.entry("worldgen", Set.of("model", "ids")));

    private static String importing(String... imports) {
        StringBuilder source = new StringBuilder("package tlm.dev.lectern.diff.x;\n\n");
        for (String imported : imports) {
            source.append("import ").append(imported).append(";\n");
        }
        return source.append("\nclass X {\n}\n").toString();
    }

    @Test
    void familyImportingAnotherFamilyIsReported() {
        String source = importing("tlm.dev.lectern.diff.blocks.BlockDiffer");
        assertEquals(
                List.of("registries/RegistryDiffer.java: registries -> blocks (tlm.dev.lectern.diff.blocks.BlockDiffer)"),
                SeamRules.violations("registries/RegistryDiffer.java", source));
    }

    @Test
    void everyFamilyMayImportOnlyWhatItDeclaresAllowed() {
        for (String from : FAMILIES) {
            for (String to : FAMILIES) {
                String source = importing("tlm.dev.lectern.diff." + to + ".Thing");
                boolean allowed = from.equals(to) || ALLOWED.get(from).contains(to);
                int expected = allowed ? 0 : 1;
                assertEquals(expected, SeamRules.violations(from + "/Source.java", source).size(), from + " -> " + to);
            }
        }
    }

    @Test
    void declaredFamiliesCoverTheMatrix() {
        assertEquals(Set.copyOf(FAMILIES), ALLOWED.keySet());
    }

    @Test
    void familyImportingARootClassIsReported() {
        String source = importing("tlm.dev.lectern.diff.VersionDiffService");
        assertEquals(
                List.of("write/ChangeSetWriter.java: write -> root (tlm.dev.lectern.diff.VersionDiffService)"),
                SeamRules.violations("write/ChangeSetWriter.java", source));
    }

    @Test
    void familyImportingAStaticMemberOfARootClassIsReported() {
        String source = "import static tlm.dev.lectern.diff.Main.EXIT_OK;\n";
        assertEquals(1, SeamRules.violations("read/Reader.java", source).size());
    }

    @Test
    void rootClassMayImportEveryFamily() {
        List<String> imports = FAMILIES.stream().map(family -> "tlm.dev.lectern.diff." + family + ".Thing").toList();
        assertEquals(List.of(), SeamRules.violations("Main.java", importing(imports.toArray(String[]::new))));
    }

    @Test
    void jacksonCoreInsideJsonIsAccepted() {
        String source = importing("tools.jackson.core.JsonParser", "tools.jackson.core.json.JsonFactory");
        assertEquals(List.of(), SeamRules.violations("json/JsonReader.java", source));
    }

    @Test
    void jacksonCoreOutsideJsonIsReported() {
        String source = importing("tools.jackson.core.JsonParser");
        assertEquals(
                List.of("read/Reader.java: tools.jackson.core outside json (tools.jackson.core.JsonParser)"),
                SeamRules.violations("read/Reader.java", source));
    }

    @Test
    void jacksonCoreInTheRootIsReported() {
        String source = importing("tools.jackson.core.JsonParser");
        assertEquals(1, SeamRules.violations("Main.java", source).size());
    }

    @Test
    void jacksonOfTheOlderLineIsAnUndeclaredThirdParty() {
        String source = importing("com.fasterxml.jackson.core.JsonParser");
        assertEquals(
                List.of("json/JsonReader.java: undeclared third-party import (com.fasterxml.jackson.core.JsonParser)"),
                SeamRules.violations("json/JsonReader.java", source));
    }

    @Test
    void undeclaredThirdPartyLibraryIsReported() {
        String source = importing("com.google.gson.Gson");
        assertEquals(
                List.of("write/Writer.java: undeclared third-party import (com.google.gson.Gson)"),
                SeamRules.violations("write/Writer.java", source));
    }

    @Test
    void jdkImportsAreAccepted() {
        String source = importing("java.util.List", "java.nio.file.Path", "javax.xml.stream.XMLStreamWriter");
        assertEquals(List.of(), SeamRules.violations("read/Reader.java", source));
    }

    @Test
    void undeclaredFamilyDirectoryIsReported() {
        assertEquals(
                List.of("misc/Thing.java: undeclared family misc"),
                SeamRules.violations("misc/Thing.java", importing("java.util.List")));
    }

    @Test
    void importOfAnUndeclaredOwnPackageIsReported() {
        String source = importing("tlm.dev.lectern.diff.misc.Thing");
        assertEquals(
                List.of("read/Reader.java: undeclared family misc (tlm.dev.lectern.diff.misc.Thing)"),
                SeamRules.violations("read/Reader.java", source));
    }

    @Test
    void importInsideATextBlockIsIgnored() {
        String source = """
                package tlm.dev.lectern.diff.read;

                class Reader {
                    String text = \"\"\"
                import tlm.dev.lectern.diff.blocks.BlockDiffer;
                \"\"\";
                }
                """;
        assertEquals(List.of(), SeamRules.violations("read/Reader.java", source));
    }

    @Test
    void wildcardImportOfAnotherFamilyIsReported() {
        String source = "import tlm.dev.lectern.diff.write.*;\n";
        assertEquals(1, SeamRules.violations("read/Reader.java", source).size());
    }

    @Test
    void qualifiedNameOfAnotherFamilyInCodeIsReported() {
        String source = """
                package tlm.dev.lectern.diff.read;

                class Reader {
                    tlm.dev.lectern.diff.write.ChangeSetWriter writer;
                }
                """;
        assertEquals(
                List.of("read/Reader.java:4: qualified name outside an import (tlm.dev.lectern.diff.)"),
                SeamRules.violations("read/Reader.java", source));
    }

    @Test
    void qualifiedJacksonNameInCodeIsReported() {
        String source = "class Reader {\n    tools.jackson.core.JsonParser parser;\n}\n";
        assertEquals(
                List.of("json/Reader.java:2: qualified name outside an import (tools.jackson.)"),
                SeamRules.violations("json/Reader.java", source));
    }

    @Test
    void qualifiedNameInAStringLiteralIsIgnored() {
        String source = "class Reader {\n    String name = \"tlm.dev.lectern.diff.write.ChangeSetWriter\";\n}\n";
        assertEquals(List.of(), SeamRules.violations("read/Reader.java", source));
    }

    @Test
    void packageDeclarationIsNotAQualifiedName() {
        String source = "package tlm.dev.lectern.diff.read;\n\nclass Reader {\n}\n";
        assertEquals(List.of(), SeamRules.violations("read/Reader.java", source));
    }
}
