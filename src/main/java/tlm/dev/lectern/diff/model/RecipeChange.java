package tlm.dev.lectern.diff.model;

public sealed interface RecipeChange extends Change permits RecipeAdded, RecipeRemoved, RecipeModified {
}
