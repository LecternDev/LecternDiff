package tlm.dev.lectern.diff.write;

import java.util.Map;
import java.util.Optional;
import tlm.dev.lectern.diff.json.JsonArray;
import tlm.dev.lectern.diff.json.JsonBoolean;
import tlm.dev.lectern.diff.json.JsonNull;
import tlm.dev.lectern.diff.json.JsonNumber;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonString;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.CommandAdded;
import tlm.dev.lectern.diff.model.CommandChange;
import tlm.dev.lectern.diff.model.CommandExecutableModified;
import tlm.dev.lectern.diff.model.CommandNodeAdded;
import tlm.dev.lectern.diff.model.CommandNodeName;
import tlm.dev.lectern.diff.model.CommandNodeRemoved;
import tlm.dev.lectern.diff.model.CommandParserModified;
import tlm.dev.lectern.diff.model.CommandPath;
import tlm.dev.lectern.diff.model.CommandPermissionsModified;
import tlm.dev.lectern.diff.model.CommandPropertiesModified;
import tlm.dev.lectern.diff.model.CommandRedirectModified;
import tlm.dev.lectern.diff.model.CommandRemoved;
import tlm.dev.lectern.diff.model.ParserProperties;
import tlm.dev.lectern.diff.model.ParserPropertyValue;
import tlm.dev.lectern.diff.model.PermissionRule;

public final class CommandChangeRenderer {

    public JsonObject render(CommandChange change) {
        return switch (change) {
            case CommandAdded added -> JsonObject.builder()
                    .put("type", "command.added")
                    .put("command", added.command().value())
                    .put("kind", added.kind().label())
                    .put("node_count", added.nodeCount())
                    .build();
            case CommandRemoved removed -> JsonObject.builder()
                    .put("type", "command.removed")
                    .put("command", removed.command().value())
                    .put("kind", removed.kind().label())
                    .put("node_count", removed.nodeCount())
                    .build();
            case CommandNodeAdded added -> JsonObject.builder()
                    .put("type", "command.node_added")
                    .put("path", path(added.path()))
                    .put("kind", added.kind().label())
                    .put("node_count", added.nodeCount())
                    .build();
            case CommandNodeRemoved removed -> JsonObject.builder()
                    .put("type", "command.node_removed")
                    .put("path", path(removed.path()))
                    .put("kind", removed.kind().label())
                    .put("node_count", removed.nodeCount())
                    .build();
            case CommandParserModified modified -> JsonObject.builder()
                    .put("type", "command.parser_modified")
                    .put("path", path(modified.path()))
                    .put("old_parser", modified.oldParser().value())
                    .put("new_parser", modified.newParser().value())
                    .build();
            case CommandPropertiesModified modified -> JsonObject.builder()
                    .put("type", "command.properties_modified")
                    .put("path", path(modified.path()))
                    .put("old_properties", properties(modified.oldProperties()))
                    .put("new_properties", properties(modified.newProperties()))
                    .build();
            case CommandExecutableModified modified -> JsonObject.builder()
                    .put("type", "command.executable_modified")
                    .put("path", path(modified.path()))
                    .put("old_executable", new JsonBoolean(modified.oldExecutable()))
                    .put("new_executable", new JsonBoolean(modified.newExecutable()))
                    .build();
            case CommandPermissionsModified modified -> JsonObject.builder()
                    .put("type", "command.permissions_modified")
                    .put("path", path(modified.path()))
                    .put("old_permissions", permissions(modified.oldPermissions()))
                    .put("new_permissions", permissions(modified.newPermissions()))
                    .build();
            case CommandRedirectModified modified -> JsonObject.builder()
                    .put("type", "command.redirect_modified")
                    .put("path", path(modified.path()))
                    .put("old_redirect", redirect(modified.oldRedirect()))
                    .put("new_redirect", redirect(modified.newRedirect()))
                    .build();
        };
    }

    private static JsonValue path(CommandPath path) {
        return new JsonArray(path.names().stream().<JsonValue>map(CommandChangeRenderer::name).toList());
    }

    private static JsonValue name(CommandNodeName name) {
        return new JsonString(name.value());
    }

    private static JsonValue redirect(Optional<CommandPath> redirect) {
        return redirect.<JsonValue>map(CommandChangeRenderer::path).orElseGet(JsonNull::new);
    }

    private static JsonValue permissions(Optional<PermissionRule> permissions) {
        return permissions.<JsonValue>map(rule -> JsonObject.builder()
                        .put("type", rule.type())
                        .put("permission", JsonObject.builder()
                                .put("type", rule.conditionType())
                                .put("level", rule.level())
                                .build())
                        .build())
                .orElseGet(JsonNull::new);
    }

    private static JsonValue properties(Optional<ParserProperties> properties) {
        return properties.<JsonValue>map(CommandChangeRenderer::propertiesObject).orElseGet(JsonNull::new);
    }

    private static JsonValue propertiesObject(ParserProperties properties) {
        JsonObject.Builder builder = JsonObject.builder();
        for (Map.Entry<String, ParserPropertyValue> property : properties.values().entrySet()) {
            builder.put(property.getKey(), switch (property.getValue()) {
                case ParserPropertyValue.Text text -> new JsonString(text.value());
                case ParserPropertyValue.Number number -> new JsonNumber(number.text());
            });
        }
        return builder.build();
    }
}
