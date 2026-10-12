package tlm.dev.lectern.diff.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.block;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.blockKey;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.blocks;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.plain;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.property;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.single;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.state;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.stateId;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.value;
import static tlm.dev.lectern.diff.fixtures.BlockFixtures.values;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.ids.IdShiftClassifier;
import tlm.dev.lectern.diff.model.Block;
import tlm.dev.lectern.diff.model.BlockAdded;
import tlm.dev.lectern.diff.model.BlockDefaultStateModified;
import tlm.dev.lectern.diff.model.BlockPropertyAdded;
import tlm.dev.lectern.diff.model.BlockPropertyRemoved;
import tlm.dev.lectern.diff.model.BlockPropertyValuesAdded;
import tlm.dev.lectern.diff.model.BlockPropertyValuesRemoved;
import tlm.dev.lectern.diff.model.BlockRemoved;
import tlm.dev.lectern.diff.model.BlockStateIdsModified;
import tlm.dev.lectern.diff.model.BlockStateIdsShifted;
import tlm.dev.lectern.diff.model.Blocks;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeSet;

class BlockDifferTest {

    private final BlockDiffer differ = new BlockDiffer(new IdShiftClassifier());

    private List<Change> diff(Blocks from, Blocks to) {
        List<Change> changes = List.copyOf(differ.diff(from, to));
        return new ChangeSet(release("1.0"), release("1.1"), changes).changes();
    }

    @Test
    void identicalBlocksGiveNoChange() {
        Blocks same = blocks(plain("demo:crate", 0), single("demo:lamp", 1, "lit", "true", "false"));
        assertEquals(List.of(), diff(same, same));
    }

    @Test
    void anAddedBlockIsOneChangeWithItsStateCountAndItsStatesAreNotListed() {
        Blocks before = blocks(plain("demo:crate", 0));
        Blocks after = blocks(plain("demo:crate", 0), single("demo:lamp", 1, "lit", "true", "false"));
        assertEquals(List.of(new BlockAdded(blockKey("demo:lamp"), 2)), diff(before, after));
    }

    @Test
    void aRemovedBlockIsOneChangeWithItsStateCount() {
        Blocks before = blocks(plain("demo:crate", 0), single("demo:lamp", 1, "lit", "true", "false"));
        Blocks after = blocks(plain("demo:crate", 0));
        assertEquals(List.of(new BlockRemoved(blockKey("demo:lamp"), 2)), diff(before, after));
    }

    @Test
    void aPropertyAddedToABlockIsReportedWithItsValuesAndItsDefaultValueAndNothingElse() {
        Blocks before = blocks(single("demo:lamp", 0, "lit", "true", "false"));
        Blocks after = blocks(block("demo:lamp")
                .property("lit", "true", "false")
                .property("dim", "low", "high")
                .state(0, "dim=low,lit=true")
                .state(1, "dim=low,lit=false")
                .state(2, "dim=high,lit=true")
                .state(3, "dim=high,lit=false")
                .defaultState("dim=low,lit=true")
                .build());
        assertEquals(
                List.of(new BlockPropertyAdded(blockKey("demo:lamp"), property("dim"), values("low", "high"), value("low"))),
                diff(before, after));
    }

    @Test
    void aPropertyRemovedFromABlockIsReportedWithItsValuesAndItsOldDefaultValueAndNothingElse() {
        Blocks before = blocks(block("demo:lamp")
                .property("lit", "true", "false")
                .property("dim", "low", "high")
                .state(0, "dim=low,lit=true")
                .state(1, "dim=low,lit=false")
                .state(2, "dim=high,lit=true")
                .state(3, "dim=high,lit=false")
                .build());
        Blocks after = blocks(single("demo:lamp", 0, "lit", "true", "false"));
        assertEquals(
                List.of(new BlockPropertyRemoved(blockKey("demo:lamp"), property("dim"), values("low", "high"), value("low"))),
                diff(before, after));
    }

    @Test
    void valuesAddedToAPropertyAtTheEndAreReportedAndShiftNothing() {
        Blocks before = blocks(single("demo:gauge", 0, "level", "0", "1"), plain("demo:crate", 2));
        Blocks after = blocks(single("demo:gauge", 0, "level", "0", "1", "2"), plain("demo:crate", 3));
        assertEquals(
                List.of(new BlockStateIdsShifted(1, 1, stateId(2)),
                        new BlockPropertyValuesAdded(blockKey("demo:gauge"), property("level"), values("2"))),
                diff(before, after));
    }

    @Test
    void aValueInsertedInTheMiddleIsReportedAndShiftsTheStatesAfterIt() {
        Blocks before = blocks(single("demo:gauge", 0, "level", "low", "high"));
        Blocks after = blocks(single("demo:gauge", 0, "level", "low", "mid", "high"));
        assertEquals(
                List.of(new BlockStateIdsShifted(1, 1, stateId(1)),
                        new BlockPropertyValuesAdded(blockKey("demo:gauge"), property("level"), values("mid"))),
                diff(before, after));
    }

    @Test
    void valuesRemovedFromAPropertyAreReportedInTheOrderOfTheOldVersion() {
        Blocks before = blocks(single("demo:gauge", 0, "level", "a", "b", "c", "d"));
        Blocks after = blocks(single("demo:gauge", 0, "level", "a", "d"));
        assertEquals(
                List.of(new BlockStateIdsShifted(1, 1, stateId(3)),
                        new BlockPropertyValuesRemoved(blockKey("demo:gauge"), property("level"), values("b", "c"))),
                diff(before, after));
    }

    @Test
    void aChangedDefaultStateIsTheOnlyChangeWhenNothingElseMoves() {
        Blocks before = blocks(block("demo:lamp").property("lit", "true", "false")
                .state(0, "lit=true").state(1, "lit=false").defaultState("lit=true").build());
        Blocks after = blocks(block("demo:lamp").property("lit", "true", "false")
                .state(0, "lit=true").state(1, "lit=false").defaultState("lit=false").build());
        assertEquals(
                List.of(new BlockDefaultStateModified(blockKey("demo:lamp"), state("lit=true"), state("lit=false"))),
                diff(before, after));
    }

    @Test
    void theDefaultStateIsOnlyComparedOnThePropertiesBothVersionsHave() {
        Blocks before = blocks(block("demo:lamp")
                .property("lit", "true", "false")
                .property("mode", "a", "b")
                .state(0, "lit=true,mode=a")
                .state(1, "lit=true,mode=b")
                .state(2, "lit=false,mode=a")
                .state(3, "lit=false,mode=b")
                .defaultState("lit=true,mode=a")
                .build());
        Blocks after = blocks(block("demo:lamp")
                .property("lit", "true", "false")
                .property("dim", "low", "high")
                .state(0, "dim=low,lit=true")
                .state(1, "dim=low,lit=false")
                .state(2, "dim=high,lit=true")
                .state(3, "dim=high,lit=false")
                .defaultState("dim=high,lit=false")
                .build());
        assertEquals(
                List.of(new BlockPropertyAdded(blockKey("demo:lamp"), property("dim"), values("low", "high"), value("high")),
                        new BlockPropertyRemoved(blockKey("demo:lamp"), property("mode"), values("a", "b"), value("a")),
                        new BlockDefaultStateModified(blockKey("demo:lamp"), state("lit=true"), state("lit=false"))),
                diff(before, after));
    }

    @Test
    void aPropertyAddedWithTheSameDefaultOnTheCommonPropertiesDoesNotModifyTheDefaultState() {
        Blocks before = blocks(block("demo:lamp").property("lit", "true", "false")
                .state(0, "lit=true").state(1, "lit=false").defaultState("lit=false").build());
        Blocks after = blocks(block("demo:lamp").property("lit", "true", "false").property("dim", "low")
                .state(0, "dim=low,lit=true").state(1, "dim=low,lit=false").defaultState("dim=low,lit=false").build());
        assertEquals(
                List.of(new BlockPropertyAdded(blockKey("demo:lamp"), property("dim"), values("low"), value("low"))),
                diff(before, after));
    }

    @Test
    void aBlockInsertedBeforeManyOthersShiftsThemAsOneAggregateNotOneChangePerBlock() {
        List<Block> beforeBlocks = new ArrayList<>();
        List<Block> afterBlocks = new ArrayList<>();
        afterBlocks.add(single("demo:aaa_new", 0, "lit", "true", "false"));
        for (int index = 0; index < 50; index++) {
            String key = "demo:block_%02d".formatted(index);
            beforeBlocks.add(single(key, index * 2, "lit", "true", "false"));
            afterBlocks.add(single(key, index * 2 + 2, "lit", "true", "false"));
        }
        assertEquals(
                List.of(new BlockStateIdsShifted(100, 50, stateId(0)), new BlockAdded(blockKey("demo:aaa_new"), 2)),
                diff(blocks(beforeBlocks.toArray(Block[]::new)), blocks(afterBlocks.toArray(Block[]::new))));
    }

    @Test
    void theShiftCountsTheBlocksAndTheStatesItTouches() {
        Blocks before = blocks(single("demo:a", 0, "lit", "true", "false"), single("demo:b", 2, "lit", "true", "false"),
                plain("demo:c", 4));
        Blocks after = blocks(single("demo:a", 0, "lit", "true", "false"), plain("demo:inserted", 2),
                single("demo:b", 3, "lit", "true", "false"), plain("demo:c", 5));
        assertEquals(
                List.of(new BlockStateIdsShifted(3, 2, stateId(2)), new BlockAdded(blockKey("demo:inserted"), 1)),
                diff(before, after));
    }

    @Test
    void aRemovedBlockShiftsTheBlocksAfterIt() {
        Blocks before = blocks(plain("demo:a", 0), plain("demo:b", 1), plain("demo:c", 2));
        Blocks after = blocks(plain("demo:a", 0), plain("demo:c", 1));
        assertEquals(
                List.of(new BlockStateIdsShifted(1, 1, stateId(2)), new BlockRemoved(blockKey("demo:b"), 1)),
                diff(before, after));
    }

    @Test
    void aBlockLosingItsPropertyShiftsTheBlocksAfterItBecauseItsStatesDisappear() {
        Blocks before = blocks(single("demo:lamp", 0, "lit", "true", "false"), plain("demo:crate", 2));
        Blocks after = blocks(plain("demo:lamp", 0), plain("demo:crate", 1));
        assertEquals(
                List.of(new BlockStateIdsShifted(1, 1, stateId(2)),
                        new BlockPropertyRemoved(blockKey("demo:lamp"), property("lit"), values("true", "false"), value("true"))),
                diff(before, after));
    }

    @Test
    void twoBlocksSwappingTheirIdsAreOneRealChangeEach() {
        Blocks before = blocks(single("demo:a", 0, "lit", "true", "false"), single("demo:b", 2, "lit", "true", "false"));
        Blocks after = blocks(single("demo:a", 2, "lit", "true", "false"), single("demo:b", 0, "lit", "true", "false"));
        assertEquals(
                List.of(new BlockStateIdsModified(blockKey("demo:a"), 2, stateId(0), stateId(2)),
                        new BlockStateIdsModified(blockKey("demo:b"), 2, stateId(2), stateId(0))),
                diff(before, after));
    }

    @Test
    void aRealChangeNextToAShiftStaysAChangeOfItsBlock() {
        Blocks before = blocks(plain("demo:a", 0), plain("demo:b", 1), plain("demo:c", 2));
        Blocks after = blocks(plain("demo:new", 0), plain("demo:a", 1), plain("demo:b", 2), plain("demo:c", 9));
        assertEquals(
                List.of(new BlockStateIdsShifted(2, 2, stateId(0)),
                        new BlockStateIdsModified(blockKey("demo:c"), 1, stateId(2), stateId(9)),
                        new BlockAdded(blockKey("demo:new"), 1)),
                diff(before, after));
    }

    @Test
    void reorderingTheStatesInsideABlockIsRealChangesOfThatBlockOnly() {
        Blocks before = blocks(block("demo:lamp").property("lit", "true", "false")
                .state(0, "lit=true").state(1, "lit=false").build(), plain("demo:crate", 2));
        Blocks after = blocks(block("demo:lamp").property("lit", "true", "false")
                .state(0, "lit=false").state(1, "lit=true").defaultState("lit=true").build(), plain("demo:crate", 2));
        assertEquals(
                List.of(new BlockStateIdsModified(blockKey("demo:lamp"), 2, stateId(0), stateId(0))),
                diff(before, after));
    }

    @Test
    void aRenameIsNeverInferredItIsARemovalAndAnAddition() {
        Blocks before = blocks(single("demo:old_name", 0, "lit", "true", "false"));
        Blocks after = blocks(single("demo:new_name", 0, "lit", "true", "false"));
        assertEquals(
                List.of(new BlockAdded(blockKey("demo:new_name"), 2), new BlockRemoved(blockKey("demo:old_name"), 2)),
                diff(before, after));
    }

    @Test
    void reversingTheComparisonMirrorsAdditionsAndRemovals() {
        Blocks before = blocks(plain("demo:a", 0));
        Blocks after = blocks(plain("demo:a", 0), plain("demo:b", 1));
        assertEquals(List.of(new BlockAdded(blockKey("demo:b"), 1)), diff(before, after));
        assertEquals(List.of(new BlockRemoved(blockKey("demo:b"), 1)), diff(after, before));
    }
}
