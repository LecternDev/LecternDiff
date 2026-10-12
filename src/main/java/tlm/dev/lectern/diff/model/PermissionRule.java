package tlm.dev.lectern.diff.model;

public record PermissionRule(String type, String conditionType, String level) {

    public PermissionRule {
        NamespacedName.require(type, "permission type");
        NamespacedName.require(conditionType, "permission condition type");
        SnakeCaseSyntax.require(level, "permission level");
    }
}
