package tlm.dev.lectern.diff.model;

import java.util.Objects;
import java.util.regex.Pattern;

public sealed interface ParserPropertyValue permits ParserPropertyValue.Text, ParserPropertyValue.Number {

    record Text(String value) implements ParserPropertyValue {

        public Text {
            Objects.requireNonNull(value, "value");
        }
    }

    record Number(String text) implements ParserPropertyValue {

        private static final Pattern SHAPE = Pattern.compile("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?");

        public Number {
            Objects.requireNonNull(text, "text");
            if (!SHAPE.matcher(text).matches()) {
                throw new IllegalArgumentException("Not a number: '" + text + "'");
            }
        }
    }
}
