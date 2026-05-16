package net.bobofraggins.tremendousstorage.storage.filingcabinet;

import net.bobofraggins.tremendousstorage.shared.util.CountFormat;
import net.bobofraggins.tremendousstorage.storage.manillafolder.FolderContents;
import net.bobofraggins.tremendousstorage.storage.manillafolder.ManillaFolderItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade (WAILA) plugin for the Filing Cabinet.
 *
 * <p>Shows:
 * <ul>
 *   <li>How many of the 8 slots are occupied
 *   <li>An item icon + count for each folder that contains items
 * </ul>
 *
 * <p>All data is read from the server NBT synced by {@link FilingCabinetBlockEntity#getUpdateTag}.
 * No separate {@code IServerDataProvider} is needed because the block entity already sends its
 * full NBT via {@code ClientboundBlockEntityDataPacket}.
 */
@WailaPlugin
public class FilingCabinetJadePlugin implements IWailaPlugin {

    static final Identifier FILING_CABINET_PROVIDER =
            Identifier.fromNamespaceAndPath("tremendousstorage", "filing_cabinet");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(FolderDataProvider.INSTANCE, FilingCabinetBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(FolderDataProvider.INSTANCE, FilingCabinetBlock.class);
    }

    // -------------------------------------------------------------------------
    // Combined server data provider + client component provider
    // -------------------------------------------------------------------------

    enum FolderDataProvider implements IBlockComponentProvider, snownee.jade.api.IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final String KEY_ITEMS = "Items";

        // -- Server side: copy the BE's NBT into the Jade data tag --

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof FilingCabinetBlockEntity be)) return;

            // Summarise each slot: store the stored-item type and total count from FolderContents
            ListTag list = new ListTag();
            for (int i = 0; i < FilingCabinetBlockEntity.SLOT_COUNT; i++) {
                ItemStack folder = be.getFolder(i);
                if (folder.isEmpty()) continue;
                FolderContents contents = ManillaFolderItem.getContents(folder);
                if (contents.isEmpty() || contents.count() == 0) continue;

                ItemStack stored = contents.storedItem().get();
                CompoundTag entry = new CompoundTag();
                entry.putString(
                        "id",
                        net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .getKey(stored.getItem())
                                .toString());
                entry.putLong("count", contents.count());
                entry.putLong("capacity", ManillaFolderItem.getCapacity(folder));
                // Serialize the full ItemStack so we can reconstruct it client-side for the icon
                net.minecraft.nbt.Tag stackTag = net.minecraft.world.item.ItemStack.OPTIONAL_CODEC
                        .encodeStart(
                                accessor.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE),
                                stored)
                        .result()
                        .orElse(new CompoundTag());
                entry.put("stack", stackTag);
                list.add(entry);
            }
            data.put(KEY_ITEMS, list);
        }

        @Override
        public Identifier getUid() {
            return FILING_CABINET_PROVIDER;
        }

        // -- Client side: render the tooltip --

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();

            net.minecraft.nbt.ListTag items = data.getListOrEmpty(KEY_ITEMS);
            if (items.isEmpty()) return;

            for (int i = 0; i < items.size(); i++) {
                CompoundTag entry = items.getCompoundOrEmpty(i);
                long count = entry.getLongOr("count", 0L);
                long capacity = entry.getLongOr("capacity", 0L);
                ItemStack stack = net.minecraft.world.item.ItemStack.OPTIONAL_CODEC
                        .parse(
                                accessor.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE),
                                entry.getCompound("stack").orElse(new CompoundTag()))
                        .result()
                        .orElse(net.minecraft.world.item.ItemStack.EMPTY);
                if (stack.isEmpty()) continue;
                tooltip.add(Component.translatable(
                        "jade.tremendousstorage.filing_cabinet.slot",
                        stack.getHoverName(),
                        formatCount(count),
                        formatCount(capacity)));
            }
        }
    }

    /** Formats a count as e.g. "4k" when ≥ 1000, or the exact number otherwise. */
    public static String formatCount(long n) {
        return CountFormat.format(n);
    }
}
