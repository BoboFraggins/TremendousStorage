package net.bobofraggins.intellistore.storage.bulkstorage;

import net.bobofraggins.intellistore.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Bulk Storage Container screen.
 *
 * <p>Carries the block position (so the client screen can read the block entity directly)
 * and a single {@link ContainerData} slot that syncs the current priority ordinal from server
 * to client.
 */
public class BulkStorageContainerMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final ContainerData data;

    /** Server-side constructor. */
    public BulkStorageContainerMenu(int id, Inventory inv, BlockPos pos, ContainerData data) {
        super(Registration.BULK_STORAGE_CONTAINER_MENU.get(), id);
        this.pos = pos;
        this.data = data;
        addDataSlots(data);
    }

    /** Client-side constructor. */
    public BulkStorageContainerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), new SimpleContainerData(1));
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getPriority() {
        return data.get(0);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
