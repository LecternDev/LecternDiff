package tlm.dev.lectern.diff.sourcerules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FamilyViolationFinder {

    private static final String BASE_DIRECTORY = "tlm/dev/lectern/diff/";
    private static final Pattern FAMILY_REFERENCE =
            Pattern.compile("\\btlm\\.dev\\.lectern\\.diff\\.([a-z][a-z0-9_]*)\\.");

    List<Violation> forbiddenReferences(List<SeamRule> seams, List<SourceFile> files) {
        Map<String, Set<String>> forbiddenByFamily = forbiddenByFamily(seams);
        List<Violation> found = new ArrayList<>();
        for (SourceFile file : files) {
            Optional<String> family = familyOf(file.path());
            if (family.isPresent() && forbiddenByFamily.containsKey(family.get())) {
                found.addAll(referencesIn(file, family.get(), forbiddenByFamily.get(family.get())));
            }
        }
        return List.copyOf(found);
    }

    List<Violation> undeclaredFamilies(List<String> declaredFamilies, List<SourceFile> files) {
        Set<String> declared = Set.copyOf(declaredFamilies);
        Map<String, String> firstFileByFamily = new LinkedHashMap<>();
        for (SourceFile file : files) {
            familyOf(file.path()).ifPresent(family -> firstFileByFamily.putIfAbsent(family, file.path()));
        }
        List<Violation> found = new ArrayList<>();
        firstFileByFamily.forEach((family, path) -> {
            if (!declared.contains(family)) {
                found.add(new Violation(path, 1, "package " + family + " is not a declared family"));
            }
        });
        return List.copyOf(found);
    }

    private static Map<String, Set<String>> forbiddenByFamily(List<SeamRule> seams) {
        Map<String, Set<String>> forbidden = new HashMap<>();
        for (SeamRule seam : seams) {
            forbidden.computeIfAbsent(seam.from(), family -> new TreeSet<>()).addAll(seam.forbidden());
        }
        return forbidden;
    }

    private static Optional<String> familyOf(String path) {
        if (!path.startsWith(BASE_DIRECTORY)) {
            return Optional.empty();
        }
        String remainder = path.substring(BASE_DIRECTORY.length());
        int slash = remainder.indexOf('/');
        return slash < 0 ? Optional.empty() : Optional.of(remainder.substring(0, slash));
    }

    private static List<Violation> referencesIn(SourceFile file, String family, Set<String> forbidden) {
        List<Violation> found = new ArrayList<>();
        String[] lines = file.text().split("\n", -1);
        for (int index = 0; index < lines.length; index++) {
            Matcher matcher = FAMILY_REFERENCE.matcher(lines[index]);
            while (matcher.find()) {
                String referenced = matcher.group(1);
                if (forbidden.contains(referenced)) {
                    String message = "family " + family + " references " + referenced
                            + " (seam " + family + " -> " + referenced + ")";
                    found.add(new Violation(file.path(), index + 1, message));
                }
            }
        }
        return found;
    }
}
