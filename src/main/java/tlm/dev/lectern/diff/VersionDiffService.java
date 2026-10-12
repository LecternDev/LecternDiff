package tlm.dev.lectern.diff;

import java.util.ArrayList;
import java.util.List;
import tlm.dev.lectern.diff.blocks.BlockDiffer;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.read.BlockReportReader;
import tlm.dev.lectern.diff.read.RegistryReportReader;
import tlm.dev.lectern.diff.read.VersionTree;
import tlm.dev.lectern.diff.registries.RegistryDiffer;

public final class VersionDiffService {

    private final RegistryReportReader registryReader;
    private final RegistryDiffer registryDiffer;
    private final BlockReportReader blockReader;
    private final BlockDiffer blockDiffer;

    public VersionDiffService(
            RegistryReportReader registryReader,
            RegistryDiffer registryDiffer,
            BlockReportReader blockReader,
            BlockDiffer blockDiffer) {
        this.registryReader = registryReader;
        this.registryDiffer = registryDiffer;
        this.blockReader = blockReader;
        this.blockDiffer = blockDiffer;
    }

    public ChangeSet diff(VersionTree from, VersionTree to) {
        List<Change> changes = new ArrayList<>();
        changes.addAll(registryDiffer.diff(registryReader.read(from), registryReader.read(to)));
        changes.addAll(blockDiffer.diff(blockReader.read(from), blockReader.read(to)));
        return new ChangeSet(from.version(), to.version(), changes);
    }
}
