package tlm.dev.lectern.diff.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.block;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.blockKey;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.blocks;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.plain;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.single;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.state;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.stateId;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BlockModelTest {

    @Test
    void propertyNamesAndValuesAcceptLowercaseDigitsAndUnderscores() {
        for (String valid : List.of("facing", "north_south", "15", "age_3")) {
            assertDoesNotThrow(() -> new PropertyName(valid), valid);
            assertDoesNotThrow(() -> new PropertyValue(valid), valid);
        }
    }

    @Test
    void propertyNamesAndValuesRefuseEverythingElse() {
        for (String invalid : List.of("", "North", "a b", "a-b", "a:b", "a=b", "a,b", "a.b")) {
            assertThrows(IllegalArgumentException.class, () -> new PropertyName(invalid), "'" + invalid + "'");
            assertThrows(IllegalArgumentException.class, () -> new PropertyValue(invalid), "'" + invalid + "'");
        }
    }

    @Test
    void blockKeysAreNamespacedLikeEveryOtherKey() {
        assertDoesNotThrow(() -> new BlockKey("demo:crate"));
        assertThrows(IllegalArgumentException.class, () -> new BlockKey("crate"));
    }

    @Test
    void stateIdsAreNeverNegative() {
        assertEquals(0, new StateId(0).value());
        assertThrows(IllegalArgumentException.class, () -> new StateId(-1));
    }

    @Test
    void aStateAssignmentIsIndependentOfTheOrderItWasBuiltIn() {
        assertEquals(state("lit=true,facing=north"), state("facing=north,lit=true"));
        assertEquals(0, state("lit=true,facing=north").compareTo(state("facing=north,lit=true")));
    }

    @Test
    void stateAssignmentsAreOrderedByNameThenValueThenLength() {
        List<StateProperties> sorted = List.of(state("b=x"), state(""), state("a=y,b=x"), state("a=x"), state("a=x,b=x")).stream()
                .sorted()
                .toList();
        assertEquals(List.of(state(""), state("a=x"), state("a=x,b=x"), state("a=y,b=x"), state("b=x")), sorted);
    }

    @Test
    void aBlockKeepsItsPropertiesAndStatesInKeyOrder() {
        Block lever = block("demo:lever")
                .property("powered", "true", "false")
                .property("face", "floor", "wall")
                .state(0, "face=floor,powered=true")
                .state(1, "face=floor,powered=false")
                .state(2, "face=wall,powered=true")
                .state(3, "face=wall,powered=false")
                .defaultState("face=wall,powered=false")
                .build();
        assertEquals(List.of(new PropertyName("face"), new PropertyName("powered")), List.copyOf(lever.properties().keySet()));
        assertEquals(List.of(new PropertyValue("true"), new PropertyValue("false")),
                lever.properties().get(new PropertyName("powered")));
        assertEquals(state("face=wall,powered=false"), lever.defaultState());
        assertEquals(stateId(1), lever.states().get(state("face=floor,powered=false")));
    }

    @Test
    void aBlockWithoutPropertiesHasOneStateWithAnEmptyAssignment() {
        Block crate = plain("demo:crate", 7);
        assertEquals(Map.of(), crate.properties());
        assertEquals(Map.of(state(""), stateId(7)), crate.states());
        assertTrue(crate.defaultState().values().isEmpty());
    }

    @Test
    void aBlockMustHaveAtLeastOneState() {
        assertThrows(IllegalArgumentException.class,
                () -> new Block(blockKey("demo:crate"), Map.of(), Map.of(), state("")));
    }

    @Test
    void theDefaultStateMustBeOneOfTheStates() {
        assertThrows(IllegalArgumentException.class,
                () -> block("demo:lamp").property("lit", "true", "false").state(0, "lit=true").defaultState("lit=false").build());
    }

    @Test
    void aStateMustSetExactlyTheDeclaredProperties() {
        assertThrows(IllegalArgumentException.class, () -> block("demo:lamp").property("lit", "true").state(0, "").build());
        assertThrows(IllegalArgumentException.class,
                () -> block("demo:lamp").property("lit", "true").state(0, "lit=true,dim=low").build());
    }

    @Test
    void aStateMayOnlyUseDeclaredValues() {
        assertThrows(IllegalArgumentException.class,
                () -> block("demo:lamp").property("lit", "true", "false").state(0, "lit=maybe").build());
    }

    @Test
    void aPropertyNeedsValuesAndNoValueTwice() {
        assertThrows(IllegalArgumentException.class, () -> block("demo:lamp").property("lit").state(0, "").build());
        assertThrows(IllegalArgumentException.class,
                () -> block("demo:lamp").property("lit", "true", "true").state(0, "lit=true").build());
    }

    @Test
    void twoStatesOfABlockCannotShareAStateId() {
        assertThrows(IllegalArgumentException.class, () -> block("demo:lamp").property("lit", "true", "false")
                .state(3, "lit=true").state(3, "lit=false").build());
    }

    @Test
    void twoBlocksCannotShareAStateId() {
        assertThrows(IllegalArgumentException.class,
                () -> blocks(plain("demo:a", 4), single("demo:b", 4, "lit", "true", "false")));
    }

    @Test
    void blocksMustBeStoredUnderTheirOwnKey() {
        Block crate = plain("demo:crate", 0);
        assertThrows(IllegalArgumentException.class, () -> new Blocks(Map.of(blockKey("demo:other"), crate)));
    }

    @Test
    void theStateCountAddsUpEveryBlock() {
        assertEquals(3, blocks(plain("demo:a", 0), single("demo:b", 1, "lit", "true", "false")).stateCount());
    }
}
