package net.bobofraggins.tremendousstorage.storage.enderbackpack;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.enderchest.EnderTremendousChestBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tremendouschest.TremendousChestBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Ender Tremendous Backpack.
 *
 * <p>Extends {@link EnderTremendousChestBlockEntity} to inherit all shared-inventory sync logic
 * (linkId, first-tick load, insert/extract sync), but overrides the storage backend to use
 * {@link EnderBackpackStorage} so backpack link IDs are kept in a separate namespace from
 * chest link IDs.
 */
public class EnderTremendousBackpackBlockEntity extends EnderTremendousChestBlockEntity {

    public EnderTremendousBackpackBlockEntity(BlockPos pos, BlockState state) {
        this(Registration.ENDER_TREMENDOUS_BACKPACK_BE_TYPE.get(), pos, state);
    }

    protected EnderTremendousBackpackBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // -------------------------------------------------------------------------
    // Storage backend override — use EnderBackpackStorage instead of EnderChestStorage
    // -------------------------------------------------------------------------

    @Override
    protected void syncTierToStorage(StorageTier tier) {
        long linkId = getLinkId();
        if (linkId == -1L || level == null || level.isClientSide()) return;
        MinecraftServer server = level.getServer();
        if (server != null) {
            EnderBackpackStorage.get(server).setTier(linkId, tier);
        }
    }

    @Override
    protected void syncCraftingUpgradeToStorage() {
        long linkId = getLinkId();
        if (linkId == -1L || level == null || level.isClientSide()) return;
        MinecraftServer server = level.getServer();
        if (server != null) {
            EnderBackpackStorage.get(server).setCraftingUpgrade(linkId);
        }
    }

    @Override
    protected void loadFromStorage() {
        long linkId = getLinkId();
        if (linkId == -1L || level == null || level.isClientSide()) return;
        MinecraftServer server = level.getServer();
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
            // Crafting upgrade: OR logic — once any linked copy has it, all get it.
            if (storage.hasCraftingUpgrade(linkId)) {
                super.setCraftingUpgrade(true); // silent; storage already has it
            } else if (hasCraftingUpgrade()) {
                storage.setCraftingUpgrade(linkId); // push local upgrade to storage
            }
        } else {
            storage.initLink(linkId, saveTypes(level.registryAccess()), getTier(), hasCraftingUpgrade());
        }
        lastKnownVersion = storage.getVersion(linkId);
    }

    @Override
    protected void syncToStorage() {
        long linkId = getLinkId();
        if (linkId == -1L || level == null || level.isClientSide()) return;
        MinecraftServer server = level.getServer();
        if (server == null) return;
        EnderBackpackStorage storage = EnderBackpackStorage.get(server);
        storage.setTypes(linkId, saveTypes(level.registryAccess()));
        lastKnownVersion = storage.getVersion(linkId);
    }

    @Override
    protected long getStorageVersion() {
        long linkId = getLinkId();
        if (linkId == -1L || level == null || level.isClientSide()) return lastKnownVersion;
        MinecraftServer server = level.getServer();
        return server == null ? lastKnownVersion : EnderBackpackStorage.get(server).getVersion(linkId);
    }

    // -------------------------------------------------------------------------
    // Display name
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        Component base = Component.translatable("block.tremendousstorage.tremendous_backpack");
        StorageTier t = getTier();
        if (t == StorageTier.WOOD) {
            return Component.empty().append(base).append(" (Ender)");
        }
        String label = Character.toUpperCase(t.getId().charAt(0)) + t.getId().substring(1);
        return Component.empty().append(base).append(" (" + label + "/Ender)");
    }

    // -------------------------------------------------------------------------
    // Menu
    // -------------------------------------------------------------------------

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        loadFromStorage();
        ContainerData data = new SimpleContainerData(1);
        return new net.bobofraggins.tremendousstorage.storage.tremendouschest.TremendousChestMenu(
                id, inv, worldPosition, data, hasCraftingUpgrade());
    }

    // -------------------------------------------------------------------------
    // Tickers
    // -------------------------------------------------------------------------

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, EnderTremendousBackpackBlockEntity be) {
        // Delegate to the chest ticker; it checks needsStorageLoad and calls be.loadFromStorage()
        // which dispatches virtually to our override above.
        EnderTremendousChestBlockEntity.serverTick(level, pos, state, be);
    }

    public static void clientTick(
            Level level, BlockPos pos, BlockState state, EnderTremendousBackpackBlockEntity be) {
        TremendousChestBlockEntity.clientTick(level, pos, state, be);
    }
}
