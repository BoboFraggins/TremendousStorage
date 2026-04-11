package net.bobofraggins.tremendousstorage.storage.wirelesshub;

import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.storage.StorageTier;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalBFS;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NiCacheHolder;
import net.bobofraggins.tremendousstorage.storage.personalaccessterminal.PersonalAccessTerminalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Block entity for the Wireless Hub.
 *
 * <p>Holds two slots:
 * <ul>
 *   <li>Slot 0 (left): accepts an unlinked {@link PersonalAccessTerminalItem}. When an item is placed
 *       here, the hub performs a BFS scan from this block to find the Network Interface.
 *       If found and the network is valid, it writes the NI position as a data component
 *       on the item and moves it to slot 1.
 *   <li>Slot 1 (right): output-only. Player retrieves the linked Wireless SAT from here.
 * </ul>
 *
 * <p>Accepts {@link StorageTier} upgrades (right-click with a StorageUpgradeItem). Each tier
 * doubles the wireless range; default (WOOD) is 16 blocks, NETHERITE is infinite.
 */
public class WirelessHubBlockEntity extends BlockEntity implements MenuProvider, NiCacheHolder {

    // -------------------------------------------------------------------------
    // Tier & range
    // -------------------------------------------------------------------------

    private StorageTier tier = StorageTier.WOOD;

    public StorageTier getTier() {
        return tier;
    }

    public void setTier(StorageTier tier) {
        this.tier = tier;
        setChanged();
    }

    /**
     * Returns the wireless range in blocks for the current tier.
     * WOOD=16, COPPER=32, IRON=64, GOLD=128, DIAMOND=256, EMERALD=512, NETHERITE=infinite ({@link Integer#MAX_VALUE}).
     */
    public int getRange() {
        if (tier == StorageTier.NETHERITE) return Integer.MAX_VALUE;
        return 16 << tier.ordinal();
    }

    // -------------------------------------------------------------------------
    // Connection state (synced to client)
    // -------------------------------------------------------------------------

    /** True when this hub is attached to an active network. Synced to client for dish animation. */
    private boolean connected = false;
    private int serverTickCounter = 0;

    public boolean isConnected() {
        return connected;
    }

    /** Called each server tick. Checks connection status every 40 ticks and syncs to client. */
    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        serverTickCounter++;
        if (serverTickCounter >= 40) {
            serverTickCounter = 0;
            boolean nowConnected = computeConnected((ServerLevel) level);
            if (nowConnected != connected) {
                connected = nowConnected;
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    private boolean computeConnected(ServerLevel level) {
        BlockPos niPos = getOrFindNiPos(level);
        if (niPos == null) return false;
        return level.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni && ni.isNetworkValid();
    }

    // -------------------------------------------------------------------------
    // Client-side animation state (not persisted)
    // -------------------------------------------------------------------------

    @Nullable
    private BlockPos cachedNiPos = null;

    /** Incremented each client tick; drives the dish spin animation. */
    private int tickCount = 0;

    /** Called each client tick by the block ticker. */
    public void clientTick() {
        tickCount++;
    }

    /**
     * Returns the dish Y rotation angle in degrees for the current frame.
     * Completes one full revolution every 40 ticks (2 seconds).
     *
     * @param partialTick fractional tick for smooth interpolation
     */
    public float getDishAngle(float partialTick) {
        return (tickCount + partialTick) % 40f / 40f * 360f;
    }

    // -------------------------------------------------------------------------
    // Inventory
    // -------------------------------------------------------------------------

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == 0 && !getStackInSlot(0).isEmpty()) {
                tryLink();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) return stack.getItem() instanceof PersonalAccessTerminalItem;
            return false; // slot 1 is output-only from external perspective
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    public WirelessHubBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.WIRELESS_HUB_BE_TYPE.get(), pos, state);
    }

    // -------------------------------------------------------------------------
    // Linking logic
    // -------------------------------------------------------------------------

    /**
     * Attempts to find a reachable Network Interface via BFS and link the item in slot 0.
     * If successful, moves the linked item to slot 1.
     */
    private void tryLink() {
        if (level == null || level.isClientSide()) return;

        ItemStack sat = inventory.getStackInSlot(0);
        if (sat.isEmpty() || !(sat.getItem() instanceof PersonalAccessTerminalItem)) return;

        // Find the nearest NI reachable from this hub
        BlockPos niPos = getOrFindNiPos((ServerLevel) level);
        if (niPos == null) return;

        if (!(level.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni)) return;
        if (!ni.isNetworkValid()) return;

        // Write the NI position and this hub's position into the item
        sat.set(Registration.WIRELESS_NI_POS.get(), niPos);
        sat.set(Registration.WIRELESS_HUB_POS.get(), worldPosition);

        // Move to output slot (slot 1)
        inventory.setStackInSlot(1, sat);
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        setChanged();
    }

    // -------------------------------------------------------------------------
    // Inventory accessor (for WirelessHubMenu)
    // -------------------------------------------------------------------------

    public IItemHandler getInventory() {
        return inventory;
    }

    // -------------------------------------------------------------------------
    // MenuProvider
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.tremendousstorage.wireless_hub");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new WirelessHubMenu(id, inv, worldPosition, inventory);
    }

    // -------------------------------------------------------------------------
    // NiCacheHolder + setChanged
    // -------------------------------------------------------------------------

    @Override
    public void invalidateNiCache() {
        cachedNiPos = null;
    }

    @Override
    @Nullable
    public BlockPos getOrFindNiPos(ServerLevel level) {
        if (cachedNiPos != null && !(level.getBlockEntity(cachedNiPos) instanceof NetworkInterfaceBlockEntity)) {
            cachedNiPos = null;
        }
        if (cachedNiPos == null) cachedNiPos = AccessTerminalBFS.findNI(level, worldPosition);
        return cachedNiPos;
    }

    @Override
    public void setChanged() {
        invalidateNiCache();
        super.setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // -------------------------------------------------------------------------
    // NBT persistence
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putString("Tier", tier.getId());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        tier = StorageTier.fromId(tag.getString("Tier"));
        connected = tag.getBoolean("Connected");
    }

    // -------------------------------------------------------------------------
    // Client sync
    // -------------------------------------------------------------------------

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Tier", tier.getId());
        tag.putBoolean("Connected", connected);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
