package tlm.dev.lectern.diff.fixtures;

public final class GadgetScenario {

    public static final String BEFORE = """
            {
              "demo:gadget": {
                "default": "demo:basic",
                "entries": {
                  "demo:basic": {"protocol_id": 0},
                  "demo:shiny": {"protocol_id": 1},
                  "demo:rusty": {"protocol_id": 2}
                },
                "protocol_id": 0
              },
              "demo:gizmo": {
                "entries": {"demo:cog": {"protocol_id": 0}},
                "protocol_id": 1
              }
            }
            """;

    public static final String AFTER = """
            {
              "demo:gadget": {
                "protocol_id": 0,
                "entries": {
                  "demo:basic": {"protocol_id": 0},
                  "demo:glossy": {"protocol_id": 1},
                  "demo:shiny": {"protocol_id": 2},
                  "demo:rusty": {"protocol_id": 3}
                },
                "default": "demo:basic"
              },
              "demo:widget": {
                "entries": {"demo:part": {"protocol_id": 0}},
                "protocol_id": 1
              }
            }
            """;

    public static final String BLOCKS_BEFORE = """
            {
              "demo:crate": {"states": [{"default": true, "id": 0}]},
              "demo:lamp": {
                "properties": {"lit": ["true", "false"]},
                "states": [
                  {"id": 1, "properties": {"lit": "true"}},
                  {"default": true, "id": 2, "properties": {"lit": "false"}}
                ]
              },
              "demo:lever": {
                "properties": {"mode": ["off", "on"]},
                "states": [
                  {"default": true, "id": 3, "properties": {"mode": "off"}},
                  {"id": 4, "properties": {"mode": "on"}}
                ]
              }
            }
            """;

    public static final String BLOCKS_AFTER = """
            {
              "demo:anvil": {"states": [{"default": true, "id": 0}]},
              "demo:crate": {"definition": {"type": "demo:plain"}, "states": [{"default": true, "id": 1}]},
              "demo:lamp": {
                "properties": {"lit": ["true", "false"], "dim": ["low", "high"]},
                "states": [
                  {"id": 2, "properties": {"dim": "low", "lit": "true"}},
                  {"default": true, "id": 3, "properties": {"dim": "low", "lit": "false"}},
                  {"id": 4, "properties": {"dim": "high", "lit": "true"}},
                  {"id": 5, "properties": {"dim": "high", "lit": "false"}}
                ]
              },
              "demo:lever": {
                "properties": {"mode": ["off", "on"]},
                "states": [
                  {"id": 6, "properties": {"mode": "off"}, "default": true},
                  {"id": 7, "properties": {"mode": "on"}}
                ]
              }
            }
            """;

    public static final String EXPECTED_JSON = """
            {
              "changes": [
                {
                  "entry": "demo:glossy",
                  "protocol_id": 1,
                  "registry": "demo:gadget",
                  "type": "registry.entry_added"
                },
                {
                  "count": 2,
                  "first_id": 1,
                  "registry": "demo:gadget",
                  "type": "registry.entry_ids_shifted"
                },
                {
                  "entry_count": 1,
                  "protocol_id": 1,
                  "registry": "demo:gizmo",
                  "type": "registry.removed"
                },
                {
                  "entry_count": 1,
                  "protocol_id": 1,
                  "registry": "demo:widget",
                  "type": "registry.added"
                },
                {
                  "block_count": 2,
                  "first_id": 0,
                  "state_count": 3,
                  "type": "block.state_ids_shifted"
                },
                {
                  "block": "demo:anvil",
                  "state_count": 1,
                  "type": "block.added"
                },
                {
                  "block": "demo:lamp",
                  "default": "low",
                  "property": "dim",
                  "type": "block.property_added",
                  "values": [
                    "low",
                    "high"
                  ]
                }
              ],
              "from": {
                "id": "1.0",
                "kind": "release"
              },
              "schema_version": 1,
              "skipped": [],
              "to": {
                "id": "1.1",
                "kind": "release"
              }
            }
            """;

    private GadgetScenario() {
    }
}
