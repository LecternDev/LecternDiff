package tlm.dev.lectern.diff.model;

import java.util.Comparator;
import java.util.Objects;

public record DataResource(DataNamespace namespace, ResourceKind kind, ResourceName name) implements Comparable<DataResource> {

    private static final Comparator<DataResource> ORDER = Comparator
            .comparing(DataResource::namespace)
            .thenComparing(DataResource::kind)
            .thenComparing(DataResource::name);

    public DataResource {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(name, "name");
    }

    public String id() {
        return namespace.value() + ":" + name.value();
    }

    public boolean isTag() {
        return kind.folder().equals(ResourceKind.TAGS_FOLDER) && kind.subfolder().isPresent();
    }

    public String registry() {
        return kind.subfolder().orElseThrow(() -> new IllegalStateException(kind + " is not a tag kind"));
    }

    @Override
    public int compareTo(DataResource other) {
        return ORDER.compare(this, other);
    }

    @Override
    public String toString() {
        return namespace + "/" + kind + "/" + name;
    }
}
