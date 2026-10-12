package tlm.dev.lectern.diff.model;

public sealed interface RegistryChange extends Change
        permits RegistryAdded, RegistryRemoved, RegistryDefaultModified, RegistryIdModified, RegistryIdsShifted,
        RegistryEntryAdded, RegistryEntryRemoved, RegistryEntryIdModified, RegistryEntryIdsShifted {
}
