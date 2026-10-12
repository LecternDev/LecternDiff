package tlm.dev.lectern.diff;

import java.util.List;
import java.util.Set;
import tlm.dev.lectern.diff.blocks.BlockDiffer;
import tlm.dev.lectern.diff.commands.CommandDiffer;
import tlm.dev.lectern.diff.ids.IdShiftClassifier;
import tlm.dev.lectern.diff.json.CanonicalJsonWriter;
import tlm.dev.lectern.diff.json.JsonReader;
import tlm.dev.lectern.diff.datafolders.UncomparedFolderService;
import tlm.dev.lectern.diff.jsondiff.JsonComparer;
import tlm.dev.lectern.diff.loottables.LootTableDiffer;
import tlm.dev.lectern.diff.model.ChangeDomain;
import tlm.dev.lectern.diff.model.ResourceKind;
import tlm.dev.lectern.diff.read.DataReader;
import tlm.dev.lectern.diff.recipes.RecipeDiffer;
import tlm.dev.lectern.diff.tags.TagDiffer;
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
import tlm.dev.lectern.diff.write.LootTableChangeRenderer;
import tlm.dev.lectern.diff.write.PacketChangeRenderer;
import tlm.dev.lectern.diff.write.PlaceChangeRenderer;
import tlm.dev.lectern.diff.write.RecipeChangeRenderer;
import tlm.dev.lectern.diff.write.RegistryChangeRenderer;
import tlm.dev.lectern.diff.write.TagChangeRenderer;

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
        DataReader dataReader = new DataReader(jsonReader);
        JsonComparer comparer = new JsonComparer();
        RecipeDiffer recipeDiffer = new RecipeDiffer(comparer);
        LootTableDiffer lootTableDiffer = new LootTableDiffer(comparer);
        TagDiffer tagDiffer = new TagDiffer();
        Set<String> comparedFolders = Set.of(ResourceKind.RECIPE.folder(), ResourceKind.LOOT_TABLE.folder(), ResourceKind.TAGS_FOLDER);
        CommandReportReader commandReader = new CommandReportReader(jsonReader);
        CommandDiffer commandDiffer = new CommandDiffer();
        return new VersionDiffService(List.of(
                new DomainComparison<>(ChangeDomain.REGISTRIES, registryReader::read, registryDiffer::diff),
                new DomainComparison<>(ChangeDomain.BLOCKS, blockReader::read, blockDiffer::diff),
                new DomainComparison<>(ChangeDomain.PACKETS, packetReader::read, packetDiffer::diff),
                new DomainComparison<>(ChangeDomain.COMMANDS, commandReader::read, commandDiffer::diff),
                new DomainComparison<>(ChangeDomain.RECIPES, dataReader::recipes, recipeDiffer::diff),
                new DomainComparison<>(ChangeDomain.LOOT_TABLES, dataReader::lootTables, lootTableDiffer::diff),
                new DomainComparison<>(ChangeDomain.TAGS, dataReader::tags, tagDiffer::diff),
                new UncomparedFoldersComparison(tree -> dataReader.otherFolders(tree, comparedFolders), new UncomparedFolderService())));
    }

    static ChangeSetWriter changeSetWriter() {
        PlaceChangeRenderer placeRenderer = new PlaceChangeRenderer();
        return new ChangeSetWriter(
                new ChangeRenderers(
                        new RegistryChangeRenderer(), new BlockChangeRenderer(), new PacketChangeRenderer(),
                        new CommandChangeRenderer(), new RecipeChangeRenderer(placeRenderer),
                        new LootTableChangeRenderer(placeRenderer), new TagChangeRenderer()),
                new CanonicalJsonWriter());
    }
}
