package tlm.dev.lectern.diff.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.json;
import static tlm.dev.lectern.diff.fixtures.DataFixtures.recipe;
import static tlm.dev.lectern.diff.fixtures.VersionFixtures.release;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tlm.dev.lectern.diff.jsondiff.JsonComparer;
import tlm.dev.lectern.diff.jsondiff.JsonPointer;
import tlm.dev.lectern.diff.jsondiff.PlaceChange;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.model.DataFiles;
import tlm.dev.lectern.diff.model.DataResource;
import tlm.dev.lectern.diff.model.Recipe;
import tlm.dev.lectern.diff.model.RecipeAdded;
import tlm.dev.lectern.diff.model.RecipeModified;
import tlm.dev.lectern.diff.model.RecipeRemoved;

class RecipeDifferTest {

    private final RecipeDiffer differ = new RecipeDiffer(new JsonComparer());

    private static Recipe smelting(String result, String extra) {
        return new Recipe("demo:smelting", Optional.of(result),
                json("{\"type\": \"demo:smelting\", \"result\": \"" + result + "\"" + extra + "}"));
    }

    private static DataFiles<Recipe> files(Map<DataResource, Recipe> recipes) {
        return new DataFiles<>(recipes);
    }

    private List<Change> diff(DataFiles<Recipe> from, DataFiles<Recipe> to) {
        return new ChangeSet(release("1.0"), release("1.1"), List.copyOf(differ.diff(from, to))).changes();
    }

    @Test
    void identicalRecipesGiveNoChange() {
        DataFiles<Recipe> same = files(Map.of(recipe("a"), smelting("demo:x", ""), recipe("b"), smelting("demo:y", ", \"time\": 200")));
        assertEquals(List.of(), diff(same, same));
    }

    @Test
    void anAddedRecipeIsOneChangeWithItsTypeAndResultAndNeverItsContent() {
        assertEquals(List.of(new RecipeAdded(recipe("b"), "demo:smelting", Optional.of("demo:y"))),
                diff(files(Map.of(recipe("a"), smelting("demo:x", ""))),
                        files(Map.of(recipe("a"), smelting("demo:x", ""), recipe("b"), smelting("demo:y", "")))));
    }

    @Test
    void aRemovedRecipeIsOneChangeWithTheSummaryOfTheOldVersion() {
        assertEquals(List.of(new RecipeRemoved(recipe("b"), "demo:smelting", Optional.of("demo:y"))),
                diff(files(Map.of(recipe("a"), smelting("demo:x", ""), recipe("b"), smelting("demo:y", ""))),
                        files(Map.of(recipe("a"), smelting("demo:x", "")))));
    }

    @Test
    void aRecipeSummaryWithoutResultKeepsAnEmptyResult() {
        Recipe special = new Recipe("demo:special", Optional.empty(), json("{\"type\": \"demo:special\"}"));
        assertEquals(List.of(new RecipeAdded(recipe("s"), "demo:special", Optional.empty())),
                diff(files(Map.of()), files(Map.of(recipe("s"), special))));
    }

    @Test
    void aModifiedRecipeListsOnlyThePlacesThatChange() {
        List<Change> changes = diff(files(Map.of(recipe("a"), smelting("demo:x", ", \"time\": 200, \"group\": \"g\""))),
                files(Map.of(recipe("a"), smelting("demo:x", ", \"time\": 100, \"group\": \"g\""))));
        assertEquals(List.of(new RecipeModified(recipe("a"),
                List.of(new PlaceChange.Modified(JsonPointer.root().child("time"), json("200"), json("100"))))), changes);
    }

    @Test
    void aRecipeWhoseOnlyDifferenceIsTheOrderOfItsKeysIsUnchanged() {
        Recipe before = new Recipe("demo:smelting", Optional.empty(), json("{\"type\": \"demo:smelting\", \"a\": 1, \"b\": [1, 2]}"));
        Recipe after = new Recipe("demo:smelting", Optional.empty(), json("{\"b\": [1, 2], \"a\": 1, \"type\": \"demo:smelting\"}"));
        assertEquals(List.of(), diff(files(Map.of(recipe("a"), before)), files(Map.of(recipe("a"), after))));
    }

    @Test
    void aNumberWrittenDifferentlyIsAChangeOfTheRecipe() {
        Recipe before = new Recipe("demo:smelting", Optional.empty(), json("{\"type\": \"demo:smelting\", \"time\": 1}"));
        Recipe after = new Recipe("demo:smelting", Optional.empty(), json("{\"type\": \"demo:smelting\", \"time\": 1.0}"));
        assertEquals(1, diff(files(Map.of(recipe("a"), before)), files(Map.of(recipe("a"), after))).size());
    }

    @Test
    void aRecipeThatChangesTypeIsModifiedAtTheTypePlaceNotRemovedAndAdded() {
        Recipe before = new Recipe("demo:smelting", Optional.of("demo:x"), json("{\"type\": \"demo:smelting\", \"result\": \"demo:x\"}"));
        Recipe after = new Recipe("demo:blasting", Optional.of("demo:x"), json("{\"type\": \"demo:blasting\", \"result\": \"demo:x\"}"));
        assertEquals(List.of(new RecipeModified(recipe("a"),
                List.of(new PlaceChange.Modified(JsonPointer.root().child("type"), json("\"demo:smelting\""), json("\"demo:blasting\""))))),
                diff(files(Map.of(recipe("a"), before)), files(Map.of(recipe("a"), after))));
    }

    @Test
    void aRenamedRecipeIsOneRemovalAndOneAdditionNeverARename() {
        Recipe same = smelting("demo:x", "");
        assertEquals(List.of(new RecipeAdded(recipe("new"), "demo:smelting", Optional.of("demo:x")),
                        new RecipeRemoved(recipe("old"), "demo:smelting", Optional.of("demo:x"))),
                diff(files(Map.of(recipe("old"), same)), files(Map.of(recipe("new"), same))));
    }
}
