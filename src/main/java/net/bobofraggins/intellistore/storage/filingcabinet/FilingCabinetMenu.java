package net.bobofraggins.intellistore.storage.filingcabinet;

import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.shared.ui.AbstractFilingCabinetMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

/**
 * Menu for the Filing Cabinet block.
 *
 * <p>Slot layout (mirrors the Personal Filing Cabinet menu):
 * <ul>
 *   <li>[0..7]   8 folder slots (only accept ManillaFolderItem, stack size 1) at x=68, y=36+i×18
 *   <li>[8..15]  8 extraction slots (read-only view of each folder's contents) at x=90, y=36+i×18
 *   <li>[16..42] Player main inventory (27 slots) at y=214
 *   <li>[43..51] Player hotbar (9 slots) at y=272
 * </ul>
 *
 * <p>ContainerData[0]: voidExcess (0=OFF, 1=ON)
 * <p>ContainerData[1]: priority ordinal (0–4)
 */
public class FilingCabinetMenu extends AbstractFilingCabinetMenu {

    private final BlockPos pos;
    private final FilingCabinetBlockEntity be;
    private final ContainerData data;

    /** Server-side constructor — called from {@link FilingCabinetBlockEntity#createMenu}. */
    public FilingCabinetMenu(int syncId, Inventory playerInv, BlockPos pos, FilingCabinetBlockEntity be) {
        super(Registration.FILING_CABINET_MENU.get(), syncId);
        this.pos = pos;
        this.be = be;

        boolean[] voidExcessHolder = {be.isVoidExcess()};
        int[] priorityHolder = {be.getPriority().ordinal()};
        this.data = new SimpleContainerData(2) {
            @Override
            public int get(int index) {
                if (index == 0) return voidExcessHolder[0] ? 1 : 0;
                if (index == 1) return priorityHolder[0];
                return 0;
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) voidExcessHolder[0] = (value != 0);
                else if (index == 1) priorityHolder[0] = value;
            }

            @Override
            public int getCount() {
                return 2;
            }
        };

        addAllSlots(be, playerInv, 214, 272);
        addDataSlots(data);
    }

    /** Client-side constructor. */
    public FilingCabinetMenu(int syncId, Inventory playerInv, FriendlyByteBuf buf) {
        super(Registration.FILING_CABINET_MENU.get(), syncId);
        this.pos = buf.readBlockPos();
        this.be = null;
        this.data = new SimpleContainerData(2);

        addAllSlots(new SimpleContainer(FOLDER_SLOTS), playerInv, 214, 272);
        addDataSlots(data);
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public boolean isVoidExcess() {
        return data.get(0) != 0;
    }

    @Override
    public void setVoidExcess(boolean on) {
        data.set(0, on ? 1 : 0);
    }

    public int getPriority() {
        return data.get(1);
    }

    @Override
    public boolean stillValid(Player player) {
        if (be == null) return true;
        return be.stillValid(player);
    }
}
