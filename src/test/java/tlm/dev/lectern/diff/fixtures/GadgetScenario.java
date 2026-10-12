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

    public static final String PACKETS_BEFORE = """
            {
              "handshake": {"serverbound": {"demo:hello": {"protocol_id": 0}}},
              "play": {
                "clientbound": {
                  "demo:ping": {"protocol_id": 0},
                  "demo:pong": {"protocol_id": 1},
                  "demo:move": {"protocol_id": 2}
                },
                "serverbound": {"demo:chat": {"protocol_id": 0}, "demo:jump": {"protocol_id": 1}}
              },
              "status": {"clientbound": {"demo:state": {"protocol_id": 0}}}
            }
            """;

    public static final String PACKETS_AFTER = """
            {
              "handshake": {"serverbound": {"demo:hello": {"protocol_id": 0}}},
              "play": {
                "clientbound": {
                  "demo:ping": {"protocol_id": 0},
                  "demo:flash": {"protocol_id": 1},
                  "demo:pong": {"protocol_id": 2},
                  "demo:move": {"protocol_id": 3}
                },
                "serverbound": {"demo:jump": {"protocol_id": 0}, "demo:chat": {"protocol_id": 1}}
              },
              "login": {"clientbound": {"demo:welcome": {"protocol_id": 0}}}
            }
            """;

    public static final String COMMANDS_BEFORE = """
            {
              "type": "root",
              "children": {
                "gadget": {
                  "type": "literal",
                  "children": {
                    "size": {
                      "type": "argument",
                      "parser": "brigadier:integer",
                      "properties": {"min": 0, "max": 3},
                      "executable": true
                    },
                    "mode": {"type": "literal", "executable": true}
                  }
                },
                "lamp": {"type": "literal", "executable": true},
                "legacy": {"type": "literal", "children": {"old": {"type": "literal", "executable": true}}}
              }
            }
            """;

    public static final String COMMANDS_AFTER = """
            {
              "type": "root",
              "children": {
                "lamp": {"type": "literal"},
                "gadget": {
                  "children": {
                    "tint": {
                      "type": "argument",
                      "parser": "minecraft:color",
                      "children": {"apply": {"type": "literal", "executable": true}}
                    },
                    "mode": {
                      "type": "literal",
                      "executable": true,
                      "permissions": {
                        "type": "minecraft:require",
                        "permission": {"type": "minecraft:command_level", "level": "gamemasters"}
                      }
                    },
                    "size": {
                      "type": "argument",
                      "parser": "brigadier:integer",
                      "properties": {"max": 9, "min": 0},
                      "executable": true
                    }
                  },
                  "type": "literal"
                },
                "dial": {
                  "type": "literal",
                  "children": {"left": {"type": "literal"}, "right": {"type": "literal"}}
                }
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
                },
                {
                  "packet_count": 1,
                  "phase": "login",
                  "type": "packet.phase_added"
                },
                {
                  "direction": "clientbound",
                  "packet": "demo:flash",
                  "phase": "play",
                  "protocol_id": 1,
                  "type": "packet.added"
                },
                {
                  "direction": "serverbound",
                  "new_id": 1,
                  "old_id": 0,
                  "packet": "demo:chat",
                  "phase": "play",
                  "type": "packet.id_modified"
                },
                {
                  "direction": "serverbound",
                  "new_id": 0,
                  "old_id": 1,
                  "packet": "demo:jump",
                  "phase": "play",
                  "type": "packet.id_modified"
                },
                {
                  "count": 2,
                  "direction": "clientbound",
                  "first_id": 1,
                  "phase": "play",
                  "type": "packet.ids_shifted"
                },
                {
                  "packet_count": 1,
                  "phase": "status",
                  "type": "packet.phase_removed"
                },
                {
                  "command": "dial",
                  "kind": "literal",
                  "node_count": 3,
                  "type": "command.added"
                },
                {
                  "new_permissions": {
                    "permission": {
                      "level": "gamemasters",
                      "type": "minecraft:command_level"
                    },
                    "type": "minecraft:require"
                  },
                  "old_permissions": null,
                  "path": [
                    "gadget",
                    "mode"
                  ],
                  "type": "command.permissions_modified"
                },
                {
                  "new_properties": {
                    "max": 9,
                    "min": 0
                  },
                  "old_properties": {
                    "max": 3,
                    "min": 0
                  },
                  "path": [
                    "gadget",
                    "size"
                  ],
                  "type": "command.properties_modified"
                },
                {
                  "kind": "argument",
                  "node_count": 2,
                  "path": [
                    "gadget",
                    "tint"
                  ],
                  "type": "command.node_added"
                },
                {
                  "new_executable": false,
                  "old_executable": true,
                  "path": [
                    "lamp"
                  ],
                  "type": "command.executable_modified"
                },
                {
                  "command": "legacy",
                  "kind": "literal",
                  "node_count": 2,
                  "type": "command.removed"
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
