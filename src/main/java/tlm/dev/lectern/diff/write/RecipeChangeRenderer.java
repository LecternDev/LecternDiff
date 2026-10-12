package tlm.dev.lectern.diff.write;

import java.util.Optional;
import tlm.dev.lectern.diff.json.JsonNull;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonString;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.RecipeAdded;
import tlm.dev.lectern.diff.model.RecipeChange;
import tlm.dev.lectern.diff.model.RecipeModified;
import tlm.dev.lectern.diff.model.RecipeRemoved;

public final class RecipeChangeRenderer {

    private final PlaceChangeRenderer places;

    public RecipeChangeRenderer(PlaceChangeRenderer places) {
        this.places = places;
    }

    public JsonObject render(RecipeChange change) {
        return switch (change) {
            case RecipeAdded added -> JsonObject.builder()
                    .put("type", "recipe.added")
                    .put("recipe", added.recipe().id())
                    .put("recipe_type", added.recipeType())
                    .put("result", text(added.result()))
                    .build();
            case RecipeRemoved removed -> JsonObject.builder()
                    .put("type", "recipe.removed")
                    .put("recipe", removed.recipe().id())
                    .put("recipe_type", removed.recipeType())
                    .put("result", text(removed.result()))
                    .build();
            case RecipeModified modified -> JsonObject.builder()
                    .put("type", "recipe.modified")
                    .put("recipe", modified.recipe().id())
                    .put("places", places.render(modified.places()))
                    .build();
        };
    }

    private static JsonValue text(Optional<String> value) {
        return value.<JsonValue>map(JsonString::new).orElseGet(JsonNull::new);
    }
}
