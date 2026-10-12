package tlm.dev.lectern.diff.read;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import tlm.dev.lectern.diff.json.JsonNumber;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.json.JsonString;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.ArgumentNode;
import tlm.dev.lectern.diff.model.CommandNode;
import tlm.dev.lectern.diff.model.CommandNodeName;
import tlm.dev.lectern.diff.model.CommandPath;
import tlm.dev.lectern.diff.model.CommandTree;
import tlm.dev.lectern.diff.model.LiteralNode;
import tlm.dev.lectern.diff.model.NodeKind;
import tlm.dev.lectern.diff.model.ParserId;
import tlm.dev.lectern.diff.model.ParserProperties;
import tlm.dev.lectern.diff.model.ParserPropertyValue;
import tlm.dev.lectern.diff.model.PermissionRule;

public final class CommandReportReader {

    public static final String FILE = "reports/commands.json";

    private static final String TYPE = "type";
    private static final String CHILDREN = "children";
    private static final String PARSER = "parser";
    private static final String PROPERTIES = "properties";
    private static final String EXECUTABLE = "executable";
    private static final String REDIRECT = "redirect";
    private static final String PERMISSIONS = "permissions";
    private static final String PERMISSION = "permission";
    private static final String LEVEL = "level";
    private static final String ROOT = "root";
    private static final Set<String> ROOT_KEYS = Set.of(TYPE, CHILDREN);
    private static final Set<String> LITERAL_KEYS = Set.of(TYPE, CHILDREN, EXECUTABLE, REDIRECT, PERMISSIONS);
    private static final Set<String> ARGUMENT_KEYS = Set.of(TYPE, CHILDREN, EXECUTABLE, REDIRECT, PERMISSIONS, PARSER, PROPERTIES);
    private static final Set<String> PERMISSIONS_KEYS = Set.of(TYPE, PERMISSION);
    private static final Set<String> CONDITION_KEYS = Set.of(TYPE, LEVEL);
    private static final Set<String> PROPERTY_NAMES = Set.of("amount", "type", "min", "max", "registry");

    private final JsonReader json;

    public CommandReportReader(JsonReader json) {
        this.json = json;
    }

    public Optional<CommandTree> read(VersionTree tree) {
        ReportContext report = new ReportContext(tree.version().id(), FILE);
        Optional<JsonValue> root = report.parse(tree, json);
        if (root.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(readTree(report, root.get()));
        } catch (IllegalArgumentException e) {
            throw report.failure(e.getMessage(), e);
        }
    }

    private CommandTree readTree(ReportContext report, JsonValue document) {
        JsonObject top = report.object(document, "the document");
        report.rejectUnknownKeys(top, ROOT_KEYS, "the root node");
        String type = report.string(report.member(top, TYPE, "the root node"), "type of the root node");
        if (!ROOT.equals(type)) {
            throw report.failure("the top node has type '" + type + "' instead of 'root'");
        }
        return new CommandTree(readChildren(report, top, "the root node", List.of()));
    }

    private Map<CommandNodeName, CommandNode> readChildren(
            ReportContext report, JsonObject parent, String parentWhere, List<CommandNodeName> prefix) {
        Map<CommandNodeName, CommandNode> children = new TreeMap<>();
        Optional<JsonValue> declared = parent.member(CHILDREN);
        if (declared.isEmpty()) {
            return children;
        }
        JsonObject object = report.object(declared.get(), "children of " + parentWhere);
        for (Map.Entry<String, JsonValue> member : object.members().entrySet()) {
            CommandNodeName name = new CommandNodeName(member.getKey());
            List<CommandNodeName> names = new ArrayList<>(prefix);
            names.add(name);
            CommandPath path = new CommandPath(names);
            children.put(name, readNode(report, report.object(member.getValue(), "node " + path), path));
        }
        return children;
    }

    private CommandNode readNode(ReportContext report, JsonObject node, CommandPath path) {
        String where = "node " + path;
        String label = report.string(report.member(node, TYPE, where), "type of " + where);
        NodeKind kind = NodeKind.ofLabel(label)
                .orElseThrow(() -> report.failure("type of " + where + " is '" + label + "' instead of 'literal' or 'argument'"));
        report.rejectUnknownKeys(node, kind == NodeKind.LITERAL ? LITERAL_KEYS : ARGUMENT_KEYS, where);
        boolean executable = node.member(EXECUTABLE).map(value -> report.bool(value, EXECUTABLE + " of " + where)).orElse(false);
        Optional<PermissionRule> permissions = node.member(PERMISSIONS).map(value -> readPermissions(report, value, where));
        Optional<CommandPath> redirect = node.member(REDIRECT).map(value -> readRedirect(report, value, where));
        Map<CommandNodeName, CommandNode> children = readChildren(report, node, where, path.names());
        return switch (kind) {
            case NodeKind.Literal _ -> new LiteralNode(executable, permissions, redirect, children);
            case NodeKind.Argument _ -> new ArgumentNode(
                    new ParserId(report.string(report.member(node, PARSER, where), PARSER + " of " + where)),
                    node.member(PROPERTIES).map(value -> readProperties(report, value, where)),
                    executable, permissions, redirect, children);
        };
    }

    private PermissionRule readPermissions(ReportContext report, JsonValue value, String where) {
        String permissionsWhere = PERMISSIONS + " of " + where;
        JsonObject permissions = report.object(value, permissionsWhere);
        report.rejectUnknownKeys(permissions, PERMISSIONS_KEYS, permissionsWhere);
        String type = report.string(report.member(permissions, TYPE, permissionsWhere), TYPE + " of " + permissionsWhere);
        String conditionWhere = PERMISSION + " of " + permissionsWhere;
        JsonObject condition = report.object(report.member(permissions, PERMISSION, permissionsWhere), conditionWhere);
        report.rejectUnknownKeys(condition, CONDITION_KEYS, conditionWhere);
        return new PermissionRule(
                type,
                report.string(report.member(condition, TYPE, conditionWhere), TYPE + " of " + conditionWhere),
                report.string(report.member(condition, LEVEL, conditionWhere), LEVEL + " of " + conditionWhere));
    }

    private CommandPath readRedirect(ReportContext report, JsonValue value, String where) {
        List<CommandNodeName> names = new ArrayList<>();
        for (JsonValue element : report.array(value, REDIRECT + " of " + where).elements()) {
            names.add(new CommandNodeName(report.string(element, "a name of " + REDIRECT + " of " + where)));
        }
        return new CommandPath(names);
    }

    private ParserProperties readProperties(ReportContext report, JsonValue value, String where) {
        String propertiesWhere = PROPERTIES + " of " + where;
        JsonObject properties = report.object(value, propertiesWhere);
        report.rejectUnknownKeys(properties, PROPERTY_NAMES, propertiesWhere);
        Map<String, ParserPropertyValue> read = new TreeMap<>();
        for (Map.Entry<String, JsonValue> property : properties.members().entrySet()) {
            read.put(property.getKey(), switch (property.getValue()) {
                case JsonString text -> new ParserPropertyValue.Text(text.value());
                case JsonNumber number -> new ParserPropertyValue.Number(number.text());
                default -> throw report.failure(property.getKey() + " of " + propertiesWhere + " is neither a string nor a number");
            });
        }
        return new ParserProperties(read);
    }
}
