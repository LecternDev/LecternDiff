package tlm.dev.lectern.diff.model;

public sealed interface CommandChange extends Change
        permits CommandAdded, CommandRemoved, CommandNodeAdded, CommandNodeRemoved, CommandParserModified,
        CommandPropertiesModified, CommandExecutableModified, CommandPermissionsModified, CommandRedirectModified {
}
