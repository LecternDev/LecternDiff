package tlm.dev.lectern.diff.model;

import java.util.Objects;
import java.util.Optional;

public record RecipeAdded(DataResource recipe, String recipeType, Optional<String> result) implements RecipeChange {

    public RecipeAdded {
        Objects.requireNonNull(recipe, "recipe");
        if (!recipe.kind().equals(ResourceKind.RECIPE)) {
            throw new IllegalArgumentException("Not a recipe: " + recipe);
        }
        NamespacedName.require(recipeType, "recipe type");
        Objects.requireNonNull(result, "result");
    }
}
