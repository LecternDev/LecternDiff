package tlm.dev.lectern.diff;

import java.util.List;
import tlm.dev.lectern.diff.blocks.BlockDiffer;
import tlm.dev.lectern.diff.commands.CommandDiffer;
import tlm.dev.lectern.diff.ids.IdShiftClassifier;
import tlm.dev.lectern.diff.json.CanonicalJsonWriter;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.model.ChangeDomain;
import tlm.dev.lectern.diff.packets.PacketDiffer;
import tlm.dev.lectern.diff.read.BlockReportReader;
import tlm.dev.lectern.diff.read.CommandReportReader;
import tlm.dev.lectern.diff.read.PacketReportReader;
import tlm.dev.lectern.diff.read.RegistryReportReader;
import tlm.dev.lectern.diff.registries.RegistryDiffer;
import tlm.dev.lectern.diff.write.BlockChangeRenderer;
import tlm.dev.lectern.diff.write.ChangeRenderers;
import tlm.dev.lectern.diff.write.ChangeSetWriter;
import tlm.dev.lectern.diff.write.CommandChangeRenderer;
import tlm.dev.lectern.diff.write.PacketChangeRenderer;
import tlm.dev.lectern.diff.write.RegistryChangeRenderer;

final class Composition {

    private Composition() {
    }

    static VersionDiffService diffService() {
        JsonReader jsonReader = new JsonReader();
        IdShiftClassifier classifier = new IdShiftClassifier();
        RegistryReportReader registryReader = new RegistryReportReader(jsonReader);
        RegistryDiffer registryDiffer = new RegistryDiffer(classifier);
        BlockReportReader blockReader = new BlockReportReader(jsonReader);
        BlockDiffer blockDiffer = new BlockDiffer(classifier);
        PacketReportReader packetReader = new PacketReportReader(jsonReader);
        PacketDiffer packetDiffer = new PacketDiffer(classifier);
        CommandReportReader commandReader = new CommandReportReader(jsonReader);
        CommandDiffer commandDiffer = new CommandDiffer();
        return new VersionDiffService(List.of(
                new DomainComparison<>(ChangeDomain.REGISTRIES, registryReader::read, registryDiffer::diff),
                new DomainComparison<>(ChangeDomain.BLOCKS, blockReader::read, blockDiffer::diff),
                new DomainComparison<>(ChangeDomain.PACKETS, packetReader::read, packetDiffer::diff),
                new DomainComparison<>(ChangeDomain.COMMANDS, commandReader::read, commandDiffer::diff)));
    }

    static ChangeSetWriter changeSetWriter() {
        return new ChangeSetWriter(
                new ChangeRenderers(
                        new RegistryChangeRenderer(), new BlockChangeRenderer(), new PacketChangeRenderer(),
                        new CommandChangeRenderer()),
                new CanonicalJsonWriter());
    }
}
