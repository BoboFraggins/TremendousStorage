package net.bobofraggins.intellistore.storage.personalfilingcabinet;

import net.bobofraggins.intellistore.shared.network.OpenPersonalFilingCabinetPacket;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.personalfilingcabinet.PersonalFilingCabinetMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Personal Filing Cabinet — a carried item that holds up to 8 Manila Folders
 * and automatically routes picked-up items into matching folders.
 *
 * <p>Right-click in air → opens the PFC inventory UI.
 * Auto-pickup is handled by {@link PersonalFilingCabinetEvents}.
 * The Ctrl+P keybind (including from a Curios slot) is handled by
 * {@link PersonalFilingCabinetClientTickHandler} + {@link OpenPersonalFilingCabinetPacket}.
 */
public class PersonalFilingCabinetItem extends Item {

    public PersonalFilingCabinetItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        int pfcSlot = findPfcSlot(player, hand);
        player.openMenu(new PersonalFilingCabinetMenu.Provider(pfcSlot, null, null),
                buf -> buf.writeInt(pfcSlot));
        return InteractionResultHolder.success(stack);
    }

    /** Returns the inventory slot index of the PFC being used (main hand or off-hand). */
    private static int findPfcSlot(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return player.getInventory().selected;
        }
        // Off-hand slot is index 40 in the full container
        return 40;
    }

    /**
     * Opens the PFC UI from a keybind, supporting both regular inventory and Curios slots.
     * Called server-side from {@link OpenPersonalFilingCabinetPacket}.
     */
    public static void openPfcUi(ServerPlayer player, int slotType, int slotIndex, String slotId) {
        if (slotType == OpenPersonalFilingCabinetPacket.SLOT_INVENTORY) {
            // Validate the slot still holds a PFC
            ItemStack stack = player.getInventory().getItem(slotIndex);
            if (!(stack.getItem() instanceof PersonalFilingCabinetItem)) return;
            player.openMenu(new PersonalFilingCabinetMenu.Provider(slotIndex, null, null),
                    buf -> buf.writeInt(slotIndex));
        } else if (slotType == OpenPersonalFilingCabinetPacket.SLOT_CURIOS) {
            openFromCurios(player, slotIndex, slotId);
        }
    }

    private static void openFromCurios(ServerPlayer player, int slotIndex, String slotId) {
        try {
            var curiosInv = player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
            if (curiosInv == null) return;
            var entry = curiosInv.getCurios().get(slotId);
            if (entry == null) return;
            var handler = entry.getStacks();
            if (slotIndex >= handler.getSlots()) return;
            ItemStack stack = handler.getStackInSlot(slotIndex);
            if (!(stack.getItem() instanceof PersonalFilingCabinetItem)) return;

            player.openMenu(
                    new PersonalFilingCabinetMenu.Provider(slotIndex, slotId, entry),
                    buf -> buf.writeInt(slotIndex));
        } catch (NoClassDefFoundError | Exception ignored) {
            // Curios not installed
        }
    }
}
