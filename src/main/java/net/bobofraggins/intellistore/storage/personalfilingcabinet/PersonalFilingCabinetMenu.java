package net.bobofraggins.intellistore.storage.personalfilingcabinet;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.shared.ui.AbstractFilingCabinetMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Personal Filing Cabinet item.
 *
 * <p>Slot layout:
 * <ul>
 *   <li>[0..7]   8 folder slots (only accept ManillaFolderItem, stack size 1) at x=29, y=44
 *   <li>[8..34]  Player main inventory (27 slots) at y=86
 *   <li>[35..43] Player hotbar (9 slots) at y=144
 * </ul>
 *
 * <p>ContainerData[0]: voidExcess (0=OFF, 1=ON)
 *
 * <p>When opened from a Curios slot, {@code curiosSlotId} and {@code curiosHandler} are
 * non-null and {@link #saveContentsToItem} writes back through the Curios handler instead
 * of the vanilla inventory.
 */
public class PersonalFilingCabinetMenu extends AbstractFilingCabinetMenu {

    private final int pfcSlot;

    @Nullable
    private final String curiosSlotId;

    @Nullable
    private final Object curiosHandler; // top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler

    private final SimpleContainer container;
    private final ContainerData data;

    /** Server-side constructor (inventory slot). */
    public PersonalFilingCabinetMenu(int syncId, Inventory playerInv, int pfcSlot) {
        this(syncId, playerInv, pfcSlot, null, null);
    }

    /** Server-side constructor supporting both inventory and Curios slots. */
    public PersonalFilingCabinetMenu(
            int syncId,
            Inventory playerInv,
            int pfcSlot,
            @Nullable String curiosSlotId,
            @Nullable Object curiosHandler) {
        super(Registration.PERSONAL_FILING_CABINET_MENU.get(), syncId);
        this.pfcSlot = pfcSlot;
        this.curiosSlotId = curiosSlotId;
        this.curiosHandler = curiosHandler;

        ItemStack pfcStack = getPfcStack(playerInv.player);
        PersonalFilingCabinetContents contents =
                pfcStack.getOrDefault(Registration.PFC_CONTENTS.get(), PersonalFilingCabinetContents.EMPTY);

        this.container = new SimpleContainer(FOLDER_SLOTS);
        for (int i = 0; i < FOLDER_SLOTS; i++) {
            container.setItem(i, contents.folders().get(i).copy());
        }

        boolean[] voidExcessHolder = {contents.voidExcess()};
        this.data = new SimpleContainerData(1) {
            @Override
            public int get(int index) {
                return voidExcessHolder[0] ? 1 : 0;
            }

            @Override
            public void set(int index, int value) {
                voidExcessHolder[0] = (value != 0);
            }

            @Override
            public int getCount() {
                return 1;
            }
        };

        addAllSlots(container, playerInv, 86, 144);
        addDataSlots(data);
    }

    /** Client-side constructor. */
    public PersonalFilingCabinetMenu(int syncId, Inventory playerInv, FriendlyByteBuf buf) {
        this(syncId, playerInv, buf.readInt());
    }

    public int getPfcSlot() {
        return pfcSlot;
    }

    @Override
    public boolean isVoidExcess() {
        return data.get(0) != 0;
    }

    @Override
    public void setVoidExcess(boolean on) {
        data.set(0, on ? 1 : 0);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer) {
            saveContentsToItem(player);
        }
    }

    /** Retrieves the PFC ItemStack from the appropriate slot (inventory or Curios). */
    private ItemStack getPfcStack(Player player) {
        if (curiosSlotId != null && curiosHandler != null) {
            try {
                var handler = (top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler) curiosHandler;
                return handler.getStacks().getStackInSlot(pfcSlot);
            } catch (NoClassDefFoundError | Exception ignored) {
            }
        }
        return player.getInventory().getItem(pfcSlot);
    }

    /** Writes the current folder container contents back to the PFC item's data component. */
    private void saveContentsToItem(Player player) {
        List<ItemStack> folders = new ArrayList<>(FOLDER_SLOTS);
        for (int i = 0; i < FOLDER_SLOTS; i++) {
            folders.add(container.getItem(i).copy());
        }
        PersonalFilingCabinetContents contents =
                new PersonalFilingCabinetContents(List.copyOf(folders), isVoidExcess());

        if (curiosSlotId != null && curiosHandler != null) {
            try {
                var handler = (top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler) curiosHandler;
                ItemStack pfcStack = handler.getStacks().getStackInSlot(pfcSlot);
                if (pfcStack.getItem() instanceof PersonalFilingCabinetItem) {
                    pfcStack.set(Registration.PFC_CONTENTS.get(), contents);
                    handler.getStacks().setStackInSlot(pfcSlot, pfcStack);
                }
            } catch (NoClassDefFoundError | Exception ignored) {
            }
            return;
        }

        ItemStack pfcStack = player.getInventory().getItem(pfcSlot);
        if (!(pfcStack.getItem() instanceof PersonalFilingCabinetItem)) return;
        pfcStack.set(Registration.PFC_CONTENTS.get(), contents);
        player.getInventory().setItem(pfcSlot, pfcStack);
    }

    // -------------------------------------------------------------------------
    // MenuProvider
    // -------------------------------------------------------------------------

    public static class Provider implements MenuProvider {
        private final int pfcSlot;

        @Nullable
        private final String curiosSlotId;

        @Nullable
        private final Object curiosHandler;

        public Provider(int pfcSlot, @Nullable String curiosSlotId, @Nullable Object curiosHandler) {
            this.pfcSlot = pfcSlot;
            this.curiosSlotId = curiosSlotId;
            this.curiosHandler = curiosHandler;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.intellistore.personal_filing_cabinet");
        }

        @Override
        public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
            return new PersonalFilingCabinetMenu(syncId, inv, pfcSlot, curiosSlotId, curiosHandler);
        }
    }
}
