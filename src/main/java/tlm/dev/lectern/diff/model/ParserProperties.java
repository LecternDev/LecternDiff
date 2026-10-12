package tlm.dev.lectern.diff.model;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public record ParserProperties(Map<String, ParserPropertyValue> values) {

    public ParserProperties {
        Map<String, ParserPropertyValue> sorted = new TreeMap<>();
        values.forEach((name, value) -> sorted.put(SnakeCaseSyntax.require(name, "parser property name"), value));
        values = Collections.unmodifiableMap(sorted);
    }
}
