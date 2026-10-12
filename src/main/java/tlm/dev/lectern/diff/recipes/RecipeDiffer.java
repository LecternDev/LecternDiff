package tlm.dev.lectern.diff.recipes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tlm.dev.lectern.diff.jsondiff.JsonComparer;
import tlm.dev.lectern.diff.jsondiff.PlaceChange;
import tlm.dev.lectern.diff.model.DataFiles;
import tlm.dev.lectern.diff.model.DataResource;
import tlm.dev.lectern.diff.model.Recipe;
import tlm.dev.lectern.diff.model.RecipeAdded;
import tlm.dev.lectern.diff.model.RecipeChange;
import tlm.dev.lectern.diff.model.RecipeModified;
import tlm.dev.lectern.diff.model.RecipeRemoved;

public final class RecipeDiffer {

    private final JsonComparer comparer;

    public RecipeDiffer(JsonComparer comparer) {
        this.comparer = comparer;
    }

    public List<RecipeChange> diff(DataFiles<Recipe> from, DataFiles<Recipe> to) {
        List<RecipeChange> changes = new ArrayList<>();
        for (Map.Entry<DataResource, Recipe> entry : from.files().entrySet()) {
            Recipe before = entry.getValue();
            Recipe after = to.files().get(entry.getKey());
            if (after == null) {
                changes.add(new RecipeRemoved(entry.getKey(), before.type(), before.result()));
                continue;
            }
            List<PlaceChange> places = comparer.compare(before.content(), after.content());
            if (!places.isEmpty()) {
                changes.add(new RecipeModified(entry.getKey(), places));
            }
        }
        for (Map.Entry<DataResource, Recipe> entry : to.files().entrySet()) {
            if (!from.files().containsKey(entry.getKey())) {
                changes.add(new RecipeAdded(entry.getKey(), entry.getValue().type(), entry.getValue().result()));
            }
        }
        return List.copyOf(changes);
    }
}
