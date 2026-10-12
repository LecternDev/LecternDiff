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
                }
              ],
              "from": {
                "id": "1.0",
                "kind": "release"
              },
              "schema_version": 1,
              "to": {
                "id": "1.1",
                "kind": "release"
              }
            }
            """;

    private GadgetScenario() {
    }
}
