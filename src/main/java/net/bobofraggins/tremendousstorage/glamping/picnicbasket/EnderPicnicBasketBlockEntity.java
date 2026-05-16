package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import net.bobofraggins.tremendousstorage.shared.priority.Priority;
import net.bobofraggins.tremendousstorage.shared.register.BETypeHelper;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.chest.ChestMenu;
import net.bobofraggins.tremendousstorage.storage.enderchest.EnderChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Block entity for the Ender Picnic Basket.
 *
 * <p>Extends {@link EnderChestBlockEntity} for shared-inventory sync behaviour. Overrides
 * storage routing to use {@link EnderPicnicBasketStorage}, and restricts accepted items to
 * edible ones only (matching {@link PicnicBasketBlockEntity}).
 */
public class EnderPicnicBasketBlockEntity extends EnderChestBlockEntity {

    private boolean autoFeed = true;

    public boolean isAutoFeed() {
        return autoFeed;
    }

    public void setAutoFeed(boolean value) {
        autoFeed = value;
        setChanged();
    }

    public EnderPicnicBasketBlockEntity(BlockPos pos, BlockState state) {
        super(BETypeHelper.get("ender_picnic_basket"), pos, state);
        setPriority(Priority.HIGH);
    }

    // -------------------------------------------------------------------------
    // Item filter
    // -------------------------------------------------------------------------

    @Override
    public boolean acceptsItem(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.FOOD);
    }

    // -------------------------------------------------------------------------
    // Upgrades
    // -------------------------------------------------------------------------

    @Override
    public boolean isUpgradeable() {
        return false;
    }

    // -------------------------------------------------------------------------
    // Storage routing — uses EnderPicnicBasketStorage instead of EnderChestStorage
    // -------------------------------------------------------------------------

    @Override
    protected void loadFromStorage() {
        long linkId = getLinkId();
        if (linkId == -1L || level == null || level.isClientSide()) return;
        MinecraftServer server = ((net.minecraft.server.level.ServerLevel) level).getServer();
        if (server == null) return;
        EnderPicnicBasketStorage storage = EnderPicnicBasketStorage.get(server);
        if (storage.hasLink(linkId)) {
            loadTypes(storage.getTypes(linkId), level.registryAccess());
        } else {
            storage.initLink(linkId, saveTypes(level.registryAccess()));
        }
        lastKnownVersion = storage.getVersion(linkId);
    }

    @Override
    protected void syncToStorage() {
        long linkId = getLinkId();
        if (linkId == -1L || level == null || level.isClientSide()) return;
        MinecraftServer server = ((net.minecraft.server.level.ServerLevel) level).getServer();
        if (server == null) return;
        EnderPicnicBasketStorage storage = EnderPicnicBasketStorage.get(server);
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
                : EnderPicnicBasketStorage.get(server).getVersion(linkId);
    }

    @Override
    protected void syncTierToStorage(StorageTier tier) {
        // no-op: picnic basket has no tiers
    }

    // -------------------------------------------------------------------------
    // Menu / display
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tremendousstorage.picnic_basket");
    }

    @Override
    public String getNetworkName() {
        return Component.translatable("block.tremendousstorage.picnic_basket").getString() + buildSuffix(true);
    }

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
        return new ChestMenu(id, inv, worldPosition, data, false, hasPullerUpgrade());
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("AutoFeed", autoFeed);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        autoFeed = input.getBooleanOr("AutoFeed", true);
    }

    // -------------------------------------------------------------------------
    // Tickers
    // -------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnderPicnicBasketBlockEntity be) {
        EnderChestBlockEntity.serverTick(level, pos, state, be);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, EnderPicnicBasketBlockEntity be) {
        EnderChestBlockEntity.clientTick(level, pos, state, be);
    }
}
