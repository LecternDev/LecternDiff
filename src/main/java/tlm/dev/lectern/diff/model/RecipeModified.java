package tlm.dev.lectern.diff.model;

import java.util.List;
import java.util.Objects;
import tlm.dev.lectern.diff.jsondiff.PlaceChange;

public record RecipeModified(DataResource recipe, List<PlaceChange> places) implements RecipeChange {

    public RecipeModified {
        Objects.requireNonNull(recipe, "recipe");
        if (!recipe.kind().equals(ResourceKind.RECIPE)) {
            throw new IllegalArgumentException("Not a recipe: " + recipe);
        }
        places = List.copyOf(places);
        if (places.isEmpty()) {
            throw new IllegalArgumentException("The recipe " + recipe + " has no changed place");
        }
    }
}
