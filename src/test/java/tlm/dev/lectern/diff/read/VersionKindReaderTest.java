package tlm.dev.lectern.diff.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.model.VersionId;
import tlm.dev.lectern.diff.model.VersionKind;

class VersionKindReaderTest {

    private final VersionKindReader reader = new VersionKindReader();

    private VersionKind kindOf(String id) {
        return reader.read(new VersionId(id));
    }

    @Test
    void releasesInBothNamings() {
        for (String id : List.of("1.21.11", "1.21", "1.0", "1.7.10", "1.3", "26.3", "26.1", "26.1.2", "27.10.4")) {
            assertEquals(new VersionKind.Release(), kindOf(id), id);
        }
    }

    @Test
    void releaseCandidatesInBothNamings() {
        for (String id : List.of("1.21.11-rc3", "1.21-rc1", "26.3-rc-3", "26.1.1-rc-1")) {
            assertEquals(new VersionKind.ReleaseCandidate(), kindOf(id), id);
        }
    }

    @Test
    void preReleasesInBothNamings() {
        for (String id : List.of("1.21.11-pre5", "1.21-pre1", "26.3-pre-3", "27.10.4-pre-12")) {
            assertEquals(new VersionKind.PreRelease(), kindOf(id), id);
        }
    }

    @Test
    void snapshotsInBothNamings() {
        for (String id : List.of("25w46a", "13w01a", "17w13b", "26.4-snapshot-3", "26.3-snapshot-10")) {
            assertEquals(new VersionKind.Snapshot(), kindOf(id), id);
        }
    }

    @Test
    void idsOutsideTheKnownShapesAreRefusedNotGuessed() {
        for (String id : List.of("1.14 Pre-Release 1", "1.14.2 Pre-Release 4", "1.RV-Pre1", "3D Shareware v1.34",
                "23w13a_or_b", "25w14craftmine", "24w14potato", "22w13oneblockatatime", "20w14infinite", "b1.8.1",
                "a1.2.6", "rd-132211", "c0.30_01c", "inf-20100618", "26.3-snapshot", "26.3-snapshot-",
                "26.3-snapshot-0", "26.3-rc3", "26.3-pre1", "1.21.11-rc-3", "1.21.11-pre-5", "1.21.11-snapshot-1",
                "26.3-RC-1", "26w14", "26w14A", "26w1a", "126w14a", "26.03", "26.3.", "26.3-rc-03", "6.3", "126.3",
                "1.", "1.21.", "1.21.11.1", "26.3.1.1", "26.3-rc-1-extra", "latest", "snapshot", "backup")) {
            VersionReadException failure = assertThrows(VersionReadException.class, () -> kindOf(id), id);
            assertTrue(failure.getMessage().contains("'" + id + "'"), failure.getMessage());
        }
    }
}
