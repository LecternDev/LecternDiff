package tlm.dev.lectern.diff.read;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import tlm.dev.lectern.diff.json.JsonArray;
import tlm.dev.lectern.diff.json.JsonObject;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.json.JsonValue;
import tlm.dev.lectern.diff.model.Block;
import tlm.dev.lectern.diff.model.BlockKey;
import tlm.dev.lectern.diff.model.Blocks;
import tlm.dev.lectern.diff.model.PropertyName;
import tlm.dev.lectern.diff.model.PropertyValue;
import tlm.dev.lectern.diff.model.StateId;
import tlm.dev.lectern.diff.model.StateProperties;

public final class BlockReportReader {

    public static final String FILE = "reports/blocks.json";

    private static final String DEFINITION = "definition";
    private static final String PROPERTIES = "properties";
    private static final String STATES = "states";
    private static final String ID = "id";
    private static final String DEFAULT = "default";
    private static final Set<String> BLOCK_KEYS = Set.of(DEFINITION, PROPERTIES, STATES);
    private static final Set<String> STATE_KEYS = Set.of(ID, PROPERTIES, DEFAULT);

    private final JsonReader json;

    public BlockReportReader(JsonReader json) {
        this.json = json;
    }

    public Blocks read(VersionTree tree) {
        ReportContext report = new ReportContext(tree.version().id(), FILE);
        JsonValue root = report.parse(tree, json);
        try {
            return new Blocks(readBlocks(report, root));
        } catch (IllegalArgumentException e) {
            throw report.failure(e.getMessage(), e);
        }
    }

    private Map<BlockKey, Block> readBlocks(ReportContext report, JsonValue root) {
        JsonObject top = report.object(root, "the document");
        Map<BlockKey, Block> byKey = new TreeMap<>();
        for (Map.Entry<String, JsonValue> member : top.members().entrySet()) {
            BlockKey key = new BlockKey(member.getKey());
            byKey.put(key, readBlock(report, key, report.object(member.getValue(), "block " + key)));
        }
        return byKey;
    }

    private Block readBlock(ReportContext report, BlockKey key, JsonObject block) {
        String where = "block " + key;
        report.rejectUnknownKeys(block, BLOCK_KEYS, where);
        Map<PropertyName, List<PropertyValue>> properties = readProperties(report, block, where);
        JsonArray states = report.array(report.member(block, STATES, where), "states of " + where);
        String stateWhere = "a state of " + where;
        Map<StateProperties, StateId> read = new TreeMap<>();
        List<StateProperties> defaults = new ArrayList<>();
        for (JsonValue value : states.elements()) {
            JsonObject state = report.object(value, stateWhere);
            report.rejectUnknownKeys(state, STATE_KEYS, stateWhere);
            StateId id = new StateId(report.nonNegativeInt(state, ID, stateWhere));
            StateProperties assignment = readAssignment(report, state, stateWhere);
            if (read.putIfAbsent(assignment, id) != null) {
                throw report.failure(where + " lists the state " + assignment + " twice");
            }
            Optional<JsonValue> flag = state.member(DEFAULT);
            if (flag.isPresent()) {
                report.requireTrue(flag.get(), "default of " + stateWhere);
                defaults.add(assignment);
            }
        }
        if (defaults.size() != 1) {
            throw report.failure(where + " has " + (defaults.isEmpty() ? "no default state" : defaults.size() + " default states"));
        }
        return new Block(key, properties, read, defaults.get(0));
    }

    private Map<PropertyName, List<PropertyValue>> readProperties(ReportContext report, JsonObject block, String where) {
        Map<PropertyName, List<PropertyValue>> properties = new TreeMap<>();
        if (block.member(PROPERTIES).isEmpty()) {
            return properties;
        }
        JsonObject declared = report.object(block.member(PROPERTIES).get(), "properties of " + where);
        for (Map.Entry<String, JsonValue> property : declared.members().entrySet()) {
            String propertyWhere = "property " + property.getKey() + " of " + where;
            List<PropertyValue> values = new ArrayList<>();
            for (JsonValue value : report.array(property.getValue(), "values of " + propertyWhere).elements()) {
                values.add(new PropertyValue(report.string(value, "a value of " + propertyWhere)));
            }
            properties.put(new PropertyName(property.getKey()), values);
        }
        return properties;
    }

    private StateProperties readAssignment(ReportContext report, JsonObject state, String where) {
        Map<PropertyName, PropertyValue> assignment = new TreeMap<>();
        if (state.member(PROPERTIES).isEmpty()) {
            return new StateProperties(assignment);
        }
        JsonObject assigned = report.object(state.member(PROPERTIES).get(), "properties of " + where);
        for (Map.Entry<String, JsonValue> property : assigned.members().entrySet()) {
            String propertyWhere = "property " + property.getKey() + " of " + where;
            assignment.put(new PropertyName(property.getKey()),
                    new PropertyValue(report.string(property.getValue(), "the value of " + propertyWhere)));
        }
        return new StateProperties(assignment);
    }
}
