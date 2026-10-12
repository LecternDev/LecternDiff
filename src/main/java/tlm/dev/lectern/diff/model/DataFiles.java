package tlm.dev.lectern.diff.model;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public record DataFiles<T>(Map<DataResource, T> files) {

    public DataFiles {
        files = Collections.unmodifiableMap(new TreeMap<>(files));
    }
}
