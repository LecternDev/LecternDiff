package tlm.dev.lectern.diff.write;

import java.util.List;
import tlm.dev.lectern.diff.json.JsonArray;
import tlm.dev.lectern.diff.json.JsonBoolean;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonString;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.TagAdded;
import tlm.dev.lectern.diff.model.TagChange;
import tlm.dev.lectern.diff.model.TagRemoved;
import tlm.dev.lectern.diff.model.TagReplaceModified;
import tlm.dev.lectern.diff.model.TagValuesAdded;
import tlm.dev.lectern.diff.model.TagValuesRemoved;

public final class TagChangeRenderer {

    public JsonObject render(TagChange change) {
        return switch (change) {
            case TagAdded added -> JsonObject.builder()
                    .put("type", "tag.added")
                    .put("registry", added.tag().registry())
                    .put("tag", added.tag().id())
                    .put("value_count", added.valueCount())
                    .build();
            case TagRemoved removed -> JsonObject.builder()
                    .put("type", "tag.removed")
                    .put("registry", removed.tag().registry())
                    .put("tag", removed.tag().id())
                    .put("value_count", removed.valueCount())
                    .build();
            case TagValuesAdded added -> JsonObject.builder()
                    .put("type", "tag.values_added")
                    .put("registry", added.tag().registry())
                    .put("tag", added.tag().id())
                    .put("values", values(added.values()))
                    .build();
            case TagValuesRemoved removed -> JsonObject.builder()
                    .put("type", "tag.values_removed")
                    .put("registry", removed.tag().registry())
                    .put("tag", removed.tag().id())
                    .put("values", values(removed.values()))
                    .build();
            case TagReplaceModified modified -> JsonObject.builder()
                    .put("type", "tag.replace_modified")
                    .put("registry", modified.tag().registry())
                    .put("tag", modified.tag().id())
                    .put("old_replace", new JsonBoolean(modified.oldReplace()))
                    .put("new_replace", new JsonBoolean(modified.newReplace()))
                    .build();
        };
    }

    private static JsonValue values(List<String> values) {
        return new JsonArray(values.stream().<JsonValue>map(JsonString::new).toList());
    }
}
