package net.bobofraggins.tremendousstorage.storage.barrel;

import net.bobofraggins.tremendousstorage.shared.priority.Priority;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;

/**
 * Menu for the Barrel block.
 *
 * <p>No barrel-specific item slots are added here (insert/extract will be handled separately).
 * The menu carries two ContainerData values: voidExcess (index 0) and priority ordinal (index 1).
 *
 * <p>Slot layout:
 * <ul>
 *   <li>[0..26]  Player main inventory at (8, 72 + row × 18)
 *   <li>[27..35] Player hotbar at (8, 130)
 * </ul>
 */
public class BarrelMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final ContainerData data;

    /** Server-side constructor. */
    public BarrelMenu(int id, Inventory playerInv, BlockPos pos, ContainerData data) {
        super(Registration.BARREL_MENU.get(), id);
        this.pos = pos;
        this.data = data;
        addPlayerSlots(playerInv);
        addDataSlots(data);
    }

    /** Client-side constructor. */
    public BarrelMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        super(Registration.BARREL_MENU.get(), id);
        this.pos = buf.readBlockPos();
        this.data = new SimpleContainerData(2);
        addPlayerSlots(playerInv);
        addDataSlots(data);
    }

    // Inventory slot positions match Dialog(blankPane(WIDTH,10) + PlayerInventoryPane()) layout:
    // titleBar(17) + spacer(10) = 27, hotbar = 27 + 3*18 + 4 = 85, x = PlayerInventoryPane margin 20
    static final int INV_LEFT = 20;
    static final int INV_TOP = 27;
    static final int HOTBAR_TOP = 85;

    private void addPlayerSlots(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, INV_LEFT + col * 18, INV_TOP + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, INV_LEFT + col * 18, HOTBAR_TOP));
        }
    }

    public BlockPos getPos() {
        return pos;
    }

    public boolean isVoidExcess() {
        return data.get(0) != 0;
    }

    public Priority getPriority() {
        return Priority.fromOrdinal(data.get(1));
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}
