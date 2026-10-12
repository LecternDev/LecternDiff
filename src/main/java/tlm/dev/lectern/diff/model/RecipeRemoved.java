package tlm.dev.lectern.diff.model;

import java.util.Objects;
import java.util.Optional;

public record RecipeRemoved(DataResource recipe, String recipeType, Optional<String> result) implements RecipeChange {

    public RecipeRemoved {
        Objects.requireNonNull(recipe, "recipe");
        if (!recipe.kind().equals(ResourceKind.RECIPE)) {
            throw new IllegalArgumentException("Not a recipe: " + recipe);
        }
        NamespacedName.require(recipeType, "recipe type");
        Objects.requireNonNull(result, "result");
    }
}
