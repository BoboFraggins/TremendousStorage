package net.bobofraggins.tremendousstorage.glamping.dankfannypack;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.ui.AbstractFilingCabinetMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Dank Fanny Pack item — identical layout to the Filing Cabinet.
 *
 * <p>The 8 folder slots are backed by a {@link SimpleContainer} populated from the item's
 * {@link FannyPackContents} component. A {@link SimpleContainer#addListener} writes any slot
 * change back to the ItemStack immediately, keeping the component in sync.
 *
 * <p>Void-excess and priority are read from the component each tick via {@link ContainerData}
 * and changed via {@code SetFannyPack*} packets.
 */
public class DankFannyPackMenu extends AbstractFilingCabinetMenu {

    static final int INV_Y = 142;
    static final int HOTBAR_Y = 200;

    private final Player player;
    private final int slotType;
    private final int slotIndex;
    private final String slotId;
    private final ContainerData data;

    /** Server-side constructor — opened via {@link DankFannyPackItem#openFannyPackUi}. */
    public DankFannyPackMenu(int syncId, Inventory playerInv, int slotType, int slotIndex, String slotId) {
        super(Registration.DANK_FANNY_PACK_MENU.get(), syncId);
        this.player = playerInv.player;
        this.slotType = slotType;
        this.slotIndex = slotIndex;
        this.slotId = slotId;

        ItemStack stack = DankFannyPackItem.getFannyPackStack(player, slotType, slotIndex, slotId);
        FannyPackContents contents =
                stack.getOrDefault(Registration.FANNY_PACK_CONTENTS.get(), FannyPackContents.EMPTY);

        // ContainerData reads live from the item component so the server always broadcasts the
        // current value; set() is a no-op because changes arrive through dedicated packets.
        this.data = new SimpleContainerData(2) {
            @Override
            public int get(int index) {
                ItemStack s = DankFannyPackItem.getFannyPackStack(player, slotType, slotIndex, slotId);
                FannyPackContents c = s.getOrDefault(Registration.FANNY_PACK_CONTENTS.get(), FannyPackContents.EMPTY);
                if (index == 0) return c.voidExcess() ? 1 : 0;
                if (index == 1) return c.priority();
                return 0;
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 2;
            }
        };

        NonNullList<ItemStack> folderItems = contents.toItemList();
        boolean[] initialized = {false};
        SimpleContainer container = new SimpleContainer(FOLDER_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                if (initialized[0]) saveFolders();
            }
        };
        // Fill before enabling the listener so initialization changes do not trigger saves.
        for (int i = 0; i < FOLDER_SLOTS; i++)
            container.setItem(i, folderItems.get(i).copy());

        addAllSlots(container, playerInv, INV_Y, HOTBAR_Y);
        addDataSlots(data);

        // Enable saves now that slots[] is populated.
        initialized[0] = true;
    }

    /** Client-side constructor — invoked by {@link Registration#DANK_FANNY_PACK_MENU}. */
    public DankFannyPackMenu(int syncId, Inventory playerInv, FriendlyByteBuf buf) {
        super(Registration.DANK_FANNY_PACK_MENU.get(), syncId);
        this.player = null;
        this.slotType = buf.readInt();
        this.slotIndex = buf.readInt();
        this.slotId = buf.readUtf();
        this.data = new SimpleContainerData(2);

        addAllSlots(new SimpleContainer(FOLDER_SLOTS), playerInv, INV_Y, HOTBAR_Y);
        addDataSlots(data);
    }

    // -------------------------------------------------------------------------
    // AbstractFilingCabinetMenu contract
    // -------------------------------------------------------------------------

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

    /** Called by the priority packet handler to sync the client-side ContainerData immediately. */
    public void setPriorityInData(int priority) {
        data.set(1, priority);
    }

    @Override
    public boolean stillValid(Player player) {
        return !DankFannyPackItem.getFannyPackStack(player, slotType, slotIndex, slotId)
                .isEmpty();
    }

    // -------------------------------------------------------------------------
    // Slot info (used by packet handlers and screen)
    // -------------------------------------------------------------------------

    public int getSlotType() {
        return slotType;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public String getSlotId() {
        return slotId;
    }

    // -------------------------------------------------------------------------
    // Persistence — write folder slot changes back to the item
    // -------------------------------------------------------------------------

    private void saveFolders() {
        if (player == null) return;
        ItemStack fannyStack = DankFannyPackItem.getFannyPackStack(player, slotType, slotIndex, slotId);
        if (fannyStack.isEmpty()) return;
        NonNullList<ItemStack> items = NonNullList.withSize(FOLDER_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < FOLDER_SLOTS; i++) {
            items.set(i, slots.get(i).getItem().copy());
        }
        FannyPackContents current =
                fannyStack.getOrDefault(Registration.FANNY_PACK_CONTENTS.get(), FannyPackContents.EMPTY);
        fannyStack.set(Registration.FANNY_PACK_CONTENTS.get(), current.withSlots(items));
        DankFannyPackItem.setFannyPackStack(player, fannyStack, slotType, slotIndex, slotId);
    }
}
