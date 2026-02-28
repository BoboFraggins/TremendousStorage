package net.bobofraggins.intellistore.storage.wirelesshub;

import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.intellistore.storage.personalaccessterminal.PersonalAccessTerminalItem;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.accessterminal.AccessTerminalBFS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
 */
public class WirelessHubBlockEntity extends BlockEntity implements MenuProvider {

    // -------------------------------------------------------------------------
    // Client-side animation state (not persisted)
    // -------------------------------------------------------------------------

    /** Incremented each client tick; drives the arc animation. */
    private int tickCount = 0;

    /** Called each client tick by the block ticker. */
    public void clientTick() {
        tickCount++;
    }

    /**
     * Returns a 0..1 progress value for the current arc position.
     * The arc completes one full cycle every 40 ticks (2 seconds).
     *
     * @param partialTick fractional tick for smooth interpolation
     */
    public float getAnimationProgress(float partialTick) {
        return (tickCount + partialTick) % 40f / 40f;
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
        BlockPos niPos = AccessTerminalBFS.findNI((ServerLevel) level, worldPosition);
        if (niPos == null) return;

        // Validate network
        if (!(level.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni)) return;
        if (!ni.isNetworkValid()) return;

        // Write the NI position into the item
        sat.set(Registration.WIRELESS_NI_POS.get(), niPos);

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
        return Component.translatable("screen.intellistore.wireless_hub");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new WirelessHubMenu(id, inv, worldPosition, inventory);
    }

    // -------------------------------------------------------------------------
    // NBT persistence
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
    }
}
