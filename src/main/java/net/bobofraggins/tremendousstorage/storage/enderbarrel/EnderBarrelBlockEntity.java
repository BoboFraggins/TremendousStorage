package net.bobofraggins.tremendousstorage.storage.enderbarrel;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.barrel.BarrelBlockEntity;
import net.bobofraggins.tremendousstorage.storage.barrel.BarrelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Block entity for the Ender Barrel.
 *
 * <p>Extends {@link BarrelBlockEntity} and adds shared-contents behaviour: all block entities
 * that share the same {@code linkId} read from and write to a single {@link EnderBarrelStorage}
 * entry so their contents are always in sync.
 */
public class EnderBarrelBlockEntity extends BarrelBlockEntity {

    public static final String TAG_LINK_ID = "LinkId";

    private long linkId = -1L;
    private boolean needsStorageLoad = false;
    private long lastKnownVersion = -1L;

    public EnderBarrelBlockEntity(BlockPos pos, BlockState state) {
        this(Registration.ENDER_BARREL_BE_TYPE.get(), pos, state);
    }

    protected EnderBarrelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // -------------------------------------------------------------------------
    // Link ID
    // -------------------------------------------------------------------------

    public long getLinkId() {
        return linkId;
    }

    public void setLinkId(long id) {
        this.linkId = id;
    }

    // -------------------------------------------------------------------------
    // Storage sync
    // -------------------------------------------------------------------------

    private EnderBarrelStorage getStorage(MinecraftServer server) {
        return EnderBarrelStorage.get(server);
    }

    private long getStorageVersion() {
        if (linkId == -1L || level == null || level.isClientSide()) return lastKnownVersion;
        MinecraftServer server = level.getServer();
        return server == null ? lastKnownVersion : getStorage(server).getVersion(linkId);
    }

    private void loadFromStorage() {
        if (linkId == -1L || level == null || level.isClientSide()) return;
        MinecraftServer server = level.getServer();
        if (server == null) return;

        EnderBarrelStorage storage = getStorage(server);
        if (storage.hasLink(linkId)) {
            loadContents(storage.getStoredItem(linkId), storage.getCount(linkId));
            baseSlot = storage.getBaseSlot(linkId);
            compactCacheKey = null;
            compactCacheBaseSlot = -1;
            StorageTier storageTier = storage.getTier(linkId);
            StorageTier localTier = getTier();
            if (localTier.ordinal() > storageTier.ordinal()) {
                storage.setTier(linkId, localTier);
            } else {
                // Call BarrelBlockEntity.setTier directly — no ender storage re-sync
                super.setTier(storageTier);
            }
        } else {
            storage.initLink(linkId, storedItem, count, getTier());
        }
        lastKnownVersion = storage.getVersion(linkId);
    }

    private void syncToStorage() {
        if (linkId == -1L || level == null || level.isClientSide()) return;
        MinecraftServer server = level.getServer();
        if (server == null) return;
        EnderBarrelStorage storage = getStorage(server);
        storage.setContentsAndBaseSlot(linkId, storedItem, count, baseSlot);
        lastKnownVersion = storage.getVersion(linkId);
    }

    private void syncTierToStorage(StorageTier t) {
        if (linkId == -1L || level == null || level.isClientSide()) return;
        MinecraftServer server = level.getServer();
        if (server != null) getStorage(server).setTier(linkId, t);
    }

    // -------------------------------------------------------------------------
    // BarrelBlockEntity overrides
    // -------------------------------------------------------------------------

    @Override
    public void setTier(StorageTier t) {
        super.setTier(t);
        syncTierToStorage(t);
    }

    @Override
    public long insert(ItemStack stack, long amount, boolean simulate) {
        long remainder = super.insert(stack, amount, simulate);
        if (!simulate) syncToStorage();
        return remainder;
    }

    @Override
    public ItemStack extract(long amount, boolean simulate) {
        ItemStack result = super.extract(amount, simulate);
        if (!simulate) syncToStorage();
        return result;
    }

    @Override
    public void clearItem() {
        super.clearItem();
        syncToStorage();
    }

    @Override
    protected void notifyChanged() {
        super.notifyChanged();
        syncToStorage();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        loadFromStorage();
        ContainerData data = new SimpleContainerData(2) {
            @Override
            public int get(int index) {
                if (index == 0) return voidExcess ? 1 : 0;
                if (index == 1) return priority.ordinal();
                return 0;
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 2;
            }
        };
        return new BarrelMenu(id, inv, worldPosition, data, hasPullerUpgrade);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tremendousstorage.ender_barrel");
    }

    @Override
    public String getNetworkName() {
        return Component.translatable("block.tremendousstorage.barrel").getString() + buildSuffix(true);
    }

    // -------------------------------------------------------------------------
    // Tick
    // -------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnderBarrelBlockEntity be) {
        BarrelBlockEntity.serverTick(level, pos, state, be);
        if (be.needsStorageLoad) {
            be.needsStorageLoad = false;
            be.loadFromStorage();
        } else {
            long storageVersion = be.getStorageVersion();
            if (storageVersion != be.lastKnownVersion) {
                be.loadFromStorage();
            }
        }
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong(TAG_LINK_ID, linkId);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        linkId = input.getLongOr(TAG_LINK_ID, -1L);
        needsStorageLoad = (linkId != -1L);
    }
}
