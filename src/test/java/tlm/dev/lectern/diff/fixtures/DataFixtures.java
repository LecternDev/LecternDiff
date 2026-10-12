package tlm.dev.lectern.diff.fixtures;

import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.DataNamespace;
import tlm.dev.lectern.diff.model.DataResource;
import tlm.dev.lectern.diff.model.ResourceKind;
import tlm.dev.lectern.diff.model.ResourceName;

public final class DataFixtures {

    private static final JsonReader JSON = new JsonReader();

    private DataFixtures() {
    }

    public static DataResource resource(String namespace, String kind, String name) {
        return new DataResource(new DataNamespace(namespace), new ResourceKind(kind), new ResourceName(name));
    }

    public static DataResource recipe(String name) {
        return resource("demo", "recipe", name);
    }

    public static DataResource lootTable(String name) {
        return resource("demo", "loot_table", name);
    }

    public static DataResource tag(String registry, String name) {
        return resource("demo", "tags/" + registry, name);
    }

    public static JsonValue json(String text) {
        return JSON.read(text);
    }
}
