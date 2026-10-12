package tlm.dev.lectern.diff;

import java.util.ArrayList;
import java.util.List;
import tlm.dev.lectern.diff.model.Change;
import tlm.dev.lectern.diff.model.ChangeSet;
import tlm.dev.lectern.diff.model.Registries;
import tlm.dev.lectern.diff.read.RegistryReportReader;
import tlm.dev.lectern.diff.read.VersionTree;
import tlm.dev.lectern.diff.registries.RegistryDiffer;

public final class VersionDiffService {

    private final RegistryReportReader registryReader;
    private final RegistryDiffer registryDiffer;

    public VersionDiffService(RegistryReportReader registryReader, RegistryDiffer registryDiffer) {
        this.registryReader = registryReader;
        this.registryDiffer = registryDiffer;
    }

    public ChangeSet diff(VersionTree from, VersionTree to) {
        Registries before = registryReader.read(from);
        Registries after = registryReader.read(to);
        List<Change> changes = new ArrayList<>(registryDiffer.diff(before, after));
        return new ChangeSet(from.version(), to.version(), changes);
    }
}
