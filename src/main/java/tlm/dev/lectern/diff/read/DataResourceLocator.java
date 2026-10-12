package tlm.dev.lectern.diff.read;

import java.util.ArrayList;
import java.util.List;
import tlm.dev.lectern.diff.model.DataNamespace;
import tlm.dev.lectern.diff.model.DataResource;
import tlm.dev.lectern.diff.model.ResourceKind;
import tlm.dev.lectern.diff.model.ResourceName;

final class DataResourceLocator {

    private static final String JSON_SUFFIX = ".json";
    private static final String META_SUFFIX = ".mcmeta";
    private static final String WORLDGEN = "worldgen";
    private static final int FIRST_FOLDER = 2;

    private final SingularFolderNames folders;

    DataResourceLocator(SingularFolderNames folders) {
        this.folders = folders;
    }

    DataResource locate(String path, ReportContext file) {
        String[] segments = path.split("/", -1);
        if (segments.length < 4) {
            throw file.failure("the file is not inside a resource folder of a namespace");
        }
        String leaf = segments[segments.length - 1];
        String stem = stemOf(leaf, file);
        List<String> directories = new ArrayList<>();
        for (int index = FIRST_FOLDER + 1; index < segments.length - 1; index++) {
            directories.add(segments[index]);
        }
        String top = folders.singular(segments[FIRST_FOLDER]);
        List<String> kind = new ArrayList<>(List.of(top));
        if (top.equals(ResourceKind.TAGS_FOLDER)) {
            moveTagRegistry(kind, directories, file);
        }
        List<String> name = new ArrayList<>(directories);
        name.add(stem);
        try {
            return new DataResource(new DataNamespace(segments[1]), new ResourceKind(String.join("/", kind)),
                    new ResourceName(String.join("/", name)));
        } catch (IllegalArgumentException e) {
            throw file.failure(e.getMessage(), e);
        }
    }

    private void moveTagRegistry(List<String> kind, List<String> directories, ReportContext file) {
        if (directories.isEmpty()) {
            throw file.failure("a tag file is below a registry folder of tags");
        }
        String registry = folders.singular(directories.removeFirst());
        kind.add(registry);
        if (registry.equals(WORLDGEN)) {
            if (directories.isEmpty()) {
                throw file.failure("a worldgen tag file is below a registry folder of worldgen");
            }
            kind.add(directories.removeFirst());
        }
    }

    private static String stemOf(String leaf, ReportContext file) {
        if (leaf.endsWith(JSON_SUFFIX)) {
            return leaf.substring(0, leaf.length() - JSON_SUFFIX.length());
        }
        if (leaf.endsWith(META_SUFFIX)) {
            return leaf;
        }
        throw file.failure("unexpected file, only " + JSON_SUFFIX + " and " + META_SUFFIX + " files are read");
    }
}
