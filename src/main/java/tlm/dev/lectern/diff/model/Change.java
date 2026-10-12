package tlm.dev.lectern.diff.model;

public sealed interface Change permits RegistryChange, BlockChange, PacketChange {
}
