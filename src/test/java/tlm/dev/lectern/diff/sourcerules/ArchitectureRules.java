package tlm.dev.lectern.diff.sourcerules;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record ArchitectureRules(List<String> families, List<SeamRule> seams) {

    private static final String SEAM_SUFFIX = " : aucun";
    private static final String NAME = "[a-z][a-z0-9]*";
    private static final Pattern SEAM_LINE =
            Pattern.compile("^(" + NAME + ") -> (" + NAME + "(?: \\| " + NAME + ")*) : aucun$");
    private static final Pattern FAMILY_ROW = Pattern.compile("^\\|\\s*`(" + NAME + ")`\\s*\\|.*$");

    ArchitectureRules {
        families = List.copyOf(families);
        seams = List.copyOf(seams);
    }

    static ArchitectureRules parse(String text) {
        List<String> families = new ArrayList<>();
        List<SeamRule> seams = new ArrayList<>();
        for (String raw : text.split("\\R")) {
            String line = raw.strip();
            if (line.endsWith(SEAM_SUFFIX)) {
                seams.add(parseSeam(line));
            } else {
                Matcher row = FAMILY_ROW.matcher(line);
                if (row.matches()) {
                    families.add(row.group(1));
                }
            }
        }
        return new ArchitectureRules(families, seams);
    }

    List<String> undeclaredSeamFamilies() {
        Set<String> declared = Set.copyOf(families);
        Set<String> undeclared = new TreeSet<>();
        for (SeamRule seam : seams) {
            if (!declared.contains(seam.from())) {
                undeclared.add(seam.from());
            }
            for (String forbidden : seam.forbidden()) {
                if (!declared.contains(forbidden)) {
                    undeclared.add(forbidden);
                }
            }
        }
        return List.copyOf(undeclared);
    }

    private static SeamRule parseSeam(String line) {
        Matcher matcher = SEAM_LINE.matcher(line);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unreadable seam line: " + line);
        }
        return new SeamRule(matcher.group(1), List.of(matcher.group(2).split(" \\| ")));
    }
}
