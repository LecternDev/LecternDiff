package tlm.dev.lectern.diff.write;

public record ChangeRenderers(
        RegistryChangeRenderer registries, BlockChangeRenderer blocks, PacketChangeRenderer packets) {
}
