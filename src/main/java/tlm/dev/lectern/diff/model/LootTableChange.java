package tlm.dev.lectern.diff.model;

public sealed interface LootTableChange extends Change permits LootTableAdded, LootTableRemoved, LootTableModified {
}
