package tlm.dev.lectern.diff.json;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CanonicalJsonSamplesTest {

    private static final Path SAMPLES = Path.of("src/test/resources/canonical-json");
    private static final String INPUT_SUFFIX = ".input.json";
    private static final String EXPECTED_SUFFIX = ".expected.json";
    private static final int MINIMUM_CASES = 11;

    private final JsonReader reader = new JsonReader();
    private final CanonicalJsonWriter writer = new CanonicalJsonWriter();

    private static List<String> caseNames() throws IOException {
        try (Stream<Path> files = Files.list(SAMPLES)) {
            return files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(INPUT_SUFFIX))
                    .map(name -> name.substring(0, name.length() - INPUT_SUFFIX.length()))
                    .sorted()
                    .toList();
        }
    }

    private static byte[] bytes(String name, String suffix) throws IOException {
        return Files.readAllBytes(SAMPLES.resolve(name + suffix));
    }

    private byte[] canonicalBytes(byte[] json) {
        String text = new String(json, StandardCharsets.UTF_8);
        return writer.write(reader.read(text)).getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void theFolderHoldsTheExpectedNumberOfCases() throws IOException {
        assertTrue(caseNames().size() >= MINIMUM_CASES, caseNames().toString());
    }

    @Test
    void everyInputHasItsExpectedFileAndEveryExpectedFileHasItsInput() throws IOException {
        List<String> expected;
        try (Stream<Path> files = Files.list(SAMPLES)) {
            expected = files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(EXPECTED_SUFFIX))
                    .map(name -> name.substring(0, name.length() - EXPECTED_SUFFIX.length()))
                    .sorted()
                    .toList();
        }
        assertEquals(caseNames(), expected);
    }

    @Test
    void theWriterGivesTheExpectedBytesForEveryInput() throws IOException {
        for (String name : caseNames()) {
            assertArrayEquals(bytes(name, EXPECTED_SUFFIX), canonicalBytes(bytes(name, INPUT_SUFFIX)), name);
        }
    }

    @Test
    void rewritingAnExpectedFileGivesTheSameBytes() throws IOException {
        for (String name : caseNames()) {
            byte[] expected = bytes(name, EXPECTED_SUFFIX);
            assertArrayEquals(expected, canonicalBytes(expected), name);
        }
    }

    @Test
    void everyExpectedFileHasLineFeedsOnlyOneFinalNewlineAndNoByteOrderMark() throws IOException {
        for (String name : caseNames()) {
            String text = new String(bytes(name, EXPECTED_SUFFIX), StandardCharsets.UTF_8);
            assertFalse(text.contains("\r"), name);
            assertTrue(text.endsWith("\n") && !text.endsWith("\n\n"), name);
            assertFalse(text.startsWith("﻿"), name);
        }
    }

    @Test
    void theSamplesCoverTheDocumentedEdgeCases() throws IOException {
        String all = String.join("\n", caseNames().stream().map(name -> {
            try {
                return new String(bytes(name, EXPECTED_SUFFIX), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException(name, e);
            }
        }).toList());
        assertTrue(all.contains("{}") && all.contains("[]"));
        assertTrue(all.contains("0.0"));
        assertTrue(all.contains("caf" + (char) 233));
        assertTrue(all.contains("\\\""));
    }
}
