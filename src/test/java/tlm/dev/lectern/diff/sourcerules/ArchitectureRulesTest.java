package tlm.dev.lectern.diff.sourcerules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class ArchitectureRulesTest {

    @Test
    void readsFamiliesFromTheTableAndSeamsFromLinesEndingWithAucun() {
        String text = """
                # Title

                | Famille | Paquets |
                |---|---|
                | `model` | `x.model` |
                | `blocks` | `x.blocks` |

                ```
                model -> blocks : aucun
                blocks -> model | read : aucun
                ```
                """;
        ArchitectureRules rules = ArchitectureRules.parse(text);
        assertEquals(List.of("model", "blocks"), rules.families());
        assertEquals(
                List.of(new SeamRule("model", List.of("blocks")),
                        new SeamRule("blocks", List.of("model", "read"))),
                rules.seams());
    }

    @Test
    void rejectsAnUnreadableSeamLine() {
        assertThrows(IllegalArgumentException.class, () -> ArchitectureRules.parse("model => blocks : aucun\n"));
    }

    @Test
    void reportsSeamFamiliesMissingFromTheTable() {
        String text = """
                | `model` | `x.model` |

                model -> ghost : aucun
                phantom -> model : aucun
                """;
        assertEquals(List.of("ghost", "phantom"), ArchitectureRules.parse(text).undeclaredSeamFamilies());
    }
}
