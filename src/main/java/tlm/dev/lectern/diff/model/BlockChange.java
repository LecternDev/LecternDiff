package tlm.dev.lectern.diff.model;

public sealed interface BlockChange extends Change
        permits BlockAdded, BlockRemoved, BlockPropertyAdded, BlockPropertyRemoved, BlockPropertyValuesAdded,
        BlockPropertyValuesRemoved, BlockDefaultStateModified, BlockStateIdsShifted, BlockStateIdsModified {
}
