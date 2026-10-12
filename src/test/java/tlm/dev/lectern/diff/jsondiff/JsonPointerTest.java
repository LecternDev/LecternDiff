package tlm.dev.lectern.diff.jsondiff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.json.JsonNumber;

class JsonPointerTest {

    @Test
    void theRootIsTheEmptyText() {
        assertEquals("", JsonPointer.root().text());
        assertEquals(List.of(), JsonPointer.root().tokens());
    }

    @Test
    void childrenAreAppendedWithASlashEach() {
        assertEquals("/pools/0/entries", JsonPointer.root().child("pools").child(0).child("entries").text());
    }

    @Test
    void slashesAndTildesInATokenAreEscapedInTheOrderOfTheStandard() {
        assertEquals("/a~1b/c~0d/~01", JsonPointer.root().child("a/b").child("c~d").child("~1").text());
    }

    @Test
    void anEmptyTokenIsAMemberWithAnEmptyName() {
        assertEquals("/", JsonPointer.root().child("").text());
    }

    @Test
    void anArrayIndexIsNotNegative() {
        assertThrows(IllegalArgumentException.class, () -> JsonPointer.root().child(-1));
    }

    @Test
    void aModifiedPlaceRefusesTwoEqualValues() {
        JsonNumber one = new JsonNumber("1");
        assertThrows(IllegalArgumentException.class, () -> new PlaceChange.Modified(JsonPointer.root(), one, new JsonNumber("1")));
    }
}
