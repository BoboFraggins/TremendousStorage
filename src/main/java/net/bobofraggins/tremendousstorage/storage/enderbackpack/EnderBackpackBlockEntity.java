package net.bobofraggins.tremendousstorage.storage.enderbackpack;

import net.bobofraggins.tremendousstorage.shared.register.BETypeHelper;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.chest.ChestMenu;
import net.bobofraggins.tremendousstorage.storage.enderchest.EnderChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Ender Tremendous Backpack.
 *
 * <p>Extends {@link EnderChestBlockEntity} to inherit all shared-inventory sync logic
 * (linkId, first-tick load, insert/extract sync), but overrides the storage backend to use
 * {@link EnderBackpackStorage} so backpack link IDs are kept in a separate namespace from
 * chest link IDs.
 */
public class EnderBackpackBlockEntity extends EnderChestBlockEntity {

    public EnderBackpackBlockEntity(BlockPos pos, BlockState state) {
        super(BETypeHelper.get("ender_backpack"), pos, state);
    }

    protected EnderBackpackBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // -------------------------------------------------------------------------
    // Storage backend override — use EnderBackpackStorage instead of EnderChestStorage
    // -------------------------------------------------------------------------

    @Override
    protected void syncTierToStorage(StorageTier tier) {
        long linkId = getLinkId();
        if (linkId == -1L || level == null || level.isClientSide()) return;
        MinecraftServer server = ((net.minecraft.server.level.ServerLevel) level).getServer();
        if (server != null) {
            EnderBackpackStorage.get(server).setTier(linkId, tier);
        }
    }

    @Override
    protected void loadFromStorage() {
        long linkId = getLinkId();
        if (linkId == -1L || level == null || level.isClientSide()) return;
        MinecraftServer server = ((net.minecraft.server.level.ServerLevel) level).getServer();
        if (server == null) return;
        EnderBackpackStorage storage = EnderBackpackStorage.get(server);
        if (storage.hasLink(linkId)) {
            loadTypes(storage.getTypes(linkId), level.registryAccess());
            StorageTier storageTier = storage.getTier(linkId);
            StorageTier localTier = getTier();
            if (localTier.ordinal() > storageTier.ordinal()) {
                storage.setTier(linkId, localTier);
            } else {
                setTierSilent(storageTier); // apply without re-syncing to storage
            }
        } else {
            storage.initLink(linkId, saveTypes(level.registryAccess()), getTier());
        }
        lastKnownVersion = storage.getVersion(linkId);
    }

    @Override
    protected void syncToStorage() {
        long linkId = getLinkId();
        if (linkId == -1L || level == null || level.isClientSide()) return;
        MinecraftServer server = ((net.minecraft.server.level.ServerLevel) level).getServer();
        if (server == null) return;
        EnderBackpackStorage storage = EnderBackpackStorage.get(server);
        storage.setTypes(linkId, saveTypes(level.registryAccess()));
        lastKnownVersion = storage.getVersion(linkId);
    }

    @Override
    protected long getStorageVersion() {
        long linkId = getLinkId();
        if (linkId == -1L || level == null || level.isClientSide()) return lastKnownVersion;
        MinecraftServer server = ((net.minecraft.server.level.ServerLevel) level).getServer();
        return server == null
                ? lastKnownVersion
                : EnderBackpackStorage.get(server).getVersion(linkId);
    }

    // -------------------------------------------------------------------------
    // Display name
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tremendousstorage.backpack");
    }

    @Override
    public String getNetworkName() {
        return Component.translatable("block.tremendousstorage.backpack").getString() + buildSuffix(true);
    }

    // -------------------------------------------------------------------------
    // Menu
    // -------------------------------------------------------------------------

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        loadFromStorage();
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? getPriority().ordinal() : 0;
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 1;
            }
        };
        return new ChestMenu(id, inv, worldPosition, data, hasCraftingUpgrade());
    }

    // -------------------------------------------------------------------------
    // Tickers
    // -------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnderBackpackBlockEntity be) {
        // Delegate to the chest ticker; it checks needsStorageLoad and calls be.loadFromStorage()
        // which dispatches virtually to our override above.
        EnderChestBlockEntity.serverTick(level, pos, state, be);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, EnderBackpackBlockEntity be) {
        ChestBlockEntity.clientTick(level, pos, state, be);
    }
}
