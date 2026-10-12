package tlm.dev.lectern.diff.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class ModelValuesTest {

    @Test
    void versionIdKeepsTheManifestIdVerbatim() {
        assertEquals("26.4-snapshot-3", new VersionId("26.4-snapshot-3").value());
        assertEquals("1.14 Pre-Release 1", new VersionId("1.14 Pre-Release 1").toString());
    }

    @Test
    void versionIdRefusesEmptyPaddedAndPathLikeValues() {
        for (String invalid : List.of("", " 1.0", "1.0 ", "a/b", "a\\b", "a\nb")) {
            assertThrows(IllegalArgumentException.class, () -> new VersionId(invalid), "'" + invalid + "'");
        }
    }

    @Test
    void protocolIdRefusesNegativeValues() {
        assertEquals(0, new ProtocolId(0).value());
        assertThrows(IllegalArgumentException.class, () -> new ProtocolId(-1));
    }

    @Test
    void namesAcceptNamespacedPathsWithSlashesDotsAndDashes() {
        for (String valid : List.of("demo:gadget", "demo:worldgen/feature_type", "my-pack.v2:part.one-a", "brigadier:bool")) {
            assertDoesNotThrow(() -> new RegistryKey(valid), valid);
            assertDoesNotThrow(() -> new EntryKey(valid), valid);
        }
    }

    @Test
    void namesRefuseAMissingNamespaceAMissingPathUppercaseAndSpaces() {
        for (String invalid : List.of("gadget", "demo:", ":gadget", "Demo:gadget", "demo:Gadget", "demo:a b", "demo:a:b", "")) {
            assertThrows(IllegalArgumentException.class, () -> new RegistryKey(invalid), "'" + invalid + "'");
            assertThrows(IllegalArgumentException.class, () -> new EntryKey(invalid), "'" + invalid + "'");
        }
    }

    @Test
    void keysCompareByTheirText() {
        assertEquals(List.of(new EntryKey("demo:a"), new EntryKey("demo:b"), new EntryKey("demo:c")),
                List.of(new EntryKey("demo:c"), new EntryKey("demo:a"), new EntryKey("demo:b")).stream().sorted().toList());
    }

    @Test
    void everyVersionKindHasItsSharedLabel() {
        assertEquals("release", new VersionKind.Release().label());
        assertEquals("release-candidate", new VersionKind.ReleaseCandidate().label());
        assertEquals("pre-release", new VersionKind.PreRelease().label());
        assertEquals("snapshot", new VersionKind.Snapshot().label());
    }
}
