package tlm.dev.lectern.diff.model;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public record StateProperties(Map<PropertyName, PropertyValue> values) implements Comparable<StateProperties> {

    public StateProperties {
        values = Collections.unmodifiableMap(new TreeMap<>(values));
    }

    @Override
    public int compareTo(StateProperties other) {
        Iterator<Map.Entry<PropertyName, PropertyValue>> mine = values.entrySet().iterator();
        Iterator<Map.Entry<PropertyName, PropertyValue>> theirs = other.values.entrySet().iterator();
        while (mine.hasNext() && theirs.hasNext()) {
            Map.Entry<PropertyName, PropertyValue> left = mine.next();
            Map.Entry<PropertyName, PropertyValue> right = theirs.next();
            int byName = left.getKey().compareTo(right.getKey());
            if (byName != 0) {
                return byName;
            }
            int byValue = left.getValue().compareTo(right.getValue());
            if (byValue != 0) {
                return byValue;
            }
        }
        return Boolean.compare(mine.hasNext(), theirs.hasNext());
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
