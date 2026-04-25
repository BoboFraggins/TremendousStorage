package net.bobofraggins.tremendousstorage.storage.armorycabinet;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.chest.ChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Menu for the Armory Cabinet — player inventory only, no upgrades, no crafting grid.
 *
 * <p>Slot layout:
 * <ul>
 *   <li>[0..26]  Player main inventory
 *   <li>[27..35] Player hotbar
 * </ul>
 */
public class ArmoryCabinetMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final ContainerData data;

    /** Server-side constructor. */
    public ArmoryCabinetMenu(int id, Inventory inv, BlockPos pos, ContainerData data) {
        super(Registration.ARMORY_CABINET_MENU.get(), id);
        this.pos = pos;
        this.data = data;

        // Player main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 20 + col * 18, 84 + row * 18));
            }
        }
        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 20 + col * 18, 142));
        }

        addDataSlots(data);
    }

    /** Client-side constructor. */
    public ArmoryCabinetMenu(int id, Inventory inv, FriendlyByteBuf buf) {
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
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index >= 0 && index < 36) {
            BlockEntity be = player.level().getBlockEntity(pos);
            if (be instanceof ChestBlockEntity bulk) {
                long remainder = bulk.insert(stack, stack.getCount(), false);
                int moved = stack.getCount() - (int) remainder;
                if (moved > 0) stack.shrink(moved);
            }
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            if (player.level().getBlockEntity(pos) instanceof ArmoryCabinetBlockEntity be) {
                be.stopOpen(player);
            }
        }
    }
}
