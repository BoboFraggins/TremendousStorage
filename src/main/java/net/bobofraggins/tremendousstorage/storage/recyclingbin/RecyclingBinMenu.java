package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * Menu for the Recycling Bin.
 *
 * <p>Slot layout:
 * <ul>
 *   <li>0 — void slot (left pane): any item placed here is instantly destroyed
 *   <li>1 — fluid-container input (right pane, top)
 *   <li>2 — fluid-container output (right pane, bottom; output-only)
 *   <li>3–29 — player inventory
 *   <li>30–38 — hotbar
 * </ul>
 */
public class RecyclingBinMenu extends AbstractContainerMenu {

    static final int VOID_SLOT_X = 36;
    static final int VOID_SLOT_Y = 32;
    static final int FLUID_IN_X = 124;
    static final int FLUID_IN_Y = 30;
    static final int FLUID_OUT_X = 124;
    static final int FLUID_OUT_Y = 65;

    static final int INV_START_X = 8;
    private static final int INV_Y = 90;
    private static final int HOTBAR_Y = 148;

    private final BlockPos pos;

    /** Server-side constructor. */
    public RecyclingBinMenu(
            int id,
            Inventory inv,
            BlockPos pos,
            @Nullable RecyclingBinBlockEntity be,
            SimpleContainer transferContainer) {
        super(Registration.RECYCLING_BIN_MENU.get(), id);
        this.pos = pos;

        // Slot 0: void slot — anything placed here is instantly destroyed
        addSlot(new Slot(new VoidContainer(be), 0, VOID_SLOT_X, VOID_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }
        });

        // Slot 1: fluid input — accepts any item with IFluidHandlerItem
        addSlot(new Slot(transferContainer, 0, FLUID_IN_X, FLUID_IN_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getCapability(Capabilities.FluidHandler.ITEM) != null;
            }
        });

        // Slot 2: output-only — player takes the result, cannot place
        addSlot(new Slot(transferContainer, 1, FLUID_OUT_X, FLUID_OUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Player inventory (slots 3–29)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, INV_START_X + col * 18, INV_Y + row * 18));
            }
        }
        // Hotbar (slots 30–38)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, INV_START_X + col * 18, HOTBAR_Y));
        }
    }

    /** Client-side constructor — reads BlockPos from network buffer. */
    public RecyclingBinMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), null, new SimpleContainer(2));
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            if (player.level().getBlockEntity(pos) instanceof RecyclingBinBlockEntity be) {
                be.stopOpen(player);
            }
        }
    }

    static boolean isEmptyFluidContainer(ItemStack stack) {
        IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) return false;
        for (int i = 0; i < handler.getTanks(); i++) {
            if (!handler.getFluidInTank(i).isEmpty()) return false;
        }
        return true;
    }

    private static boolean isEmptyBucketOrBottle(ItemStack stack) {
        return stack.getItem() == Items.BUCKET || stack.getItem() == Items.GLASS_BOTTLE;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index == 0) {
            // Void slot: nothing to shift-click out
            return ItemStack.EMPTY;
        } else if (index <= 2) {
            // Fluid slots → player inventory + hotbar
            if (!moveItemStackTo(stack, 3, 39, false)) return ItemStack.EMPTY;
        } else if (index < 30) {
            // Player inventory → empty buckets/bottles to fluid input, everything else to void
            if (isEmptyBucketOrBottle(stack)) {
                if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 0, 1, false)) {
                    if (!moveItemStackTo(stack, 30, 39, false)) return ItemStack.EMPTY;
                }
            }
        } else {
            // Hotbar → empty buckets/bottles to fluid input, everything else to void
            if (isEmptyBucketOrBottle(stack)) {
                if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 0, 1, false)) {
                    if (!moveItemStackTo(stack, 3, 30, false)) return ItemStack.EMPTY;
                }
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else if (stack.getCount() != original.getCount()) slot.setChanged();
        else return ItemStack.EMPTY;

        return original;
    }

    // -------------------------------------------------------------------------
    // Inner: single-slot void container — notifies BE on item insertion
    // -------------------------------------------------------------------------

    private static class VoidContainer implements Container {
        @Nullable
        private final RecyclingBinBlockEntity be;

        VoidContainer(@Nullable RecyclingBinBlockEntity be) {
            this.be = be;
        }

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (!stack.isEmpty() && be != null) {
                be.onItemsDestroyed(stack.getCount());
                Level level = be.getLevel();
                if (level != null && !level.isClientSide()) {
                    level.playSound(
                            null,
                            be.getBlockPos(),
                            SoundEvents.ITEM_BREAK.value(),
                            SoundSource.BLOCKS,
                            1.0f,
                            0.9f + level.random.nextFloat() * 0.2f);
                }
            }
            // Item is not stored — it's destroyed
        }

        @Override
        public void setChanged() {}

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {}
    }
}
