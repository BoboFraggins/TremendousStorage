package net.bobofraggins.tremendousstorage.storage.personalaccessterminal;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side menu for the Wireless SAT item.
 *
 * <p>Uses the same menu type as {@link AccessTerminalMenu} so the client opens
 * {@link net.bobofraggins.tremendousstorage.shared.ui.AccessTerminalScreen} automatically.
 * Overrides {@link #stillValid} to check that the player still has a linked Wireless SAT
 * (in inventory, off-hand, or Curios slot) rather than checking for a physical SAT block.
 */
public class PersonalAccessTerminalMenu extends AccessTerminalMenu {

    /**
     * Server-side constructor.
     *
     * @param niPos the linked Network Interface position (used as both satPos and niPos)
     */
    public PersonalAccessTerminalMenu(int id, Inventory inv, BlockPos niPos) {
        // Pass niPos as both satPos and niPos — stillValid is overridden below.
        super(id, inv, niPos, niPos);
    }

    /**
     * Remains valid as long as the player has a linked Wireless SAT item in their
     * main inventory, off-hand, or a Curios slot that links to this NI.
     */
    @Override
    public boolean stillValid(Player player) {
        BlockPos niPos = getNiPos();
        if (niPos == null) return false;
        return playerHasLinkedPersonalAccessTerminal(player, niPos);
    }

    private static boolean playerHasLinkedPersonalAccessTerminal(Player player, BlockPos niPos) {
        // Check main inventory + off-hand
        for (ItemStack stack : player.getInventory().items) {
            if (isMatchingPersonalAccessTerminal(stack, niPos)) return true;
        }
        if (isMatchingPersonalAccessTerminal(player.getOffhandItem(), niPos)) return true;

        // Check Curios slots (soft dependency)
        try {
            var curiosInv = player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
            if (curiosInv != null) {
                for (var entry : curiosInv.getCurios().entrySet()) {
                    var handler = entry.getValue().getStacks();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        if (isMatchingPersonalAccessTerminal(handler.getStackInSlot(i), niPos)) return true;
                    }
                }
            }
        } catch (NoClassDefFoundError | Exception ignored) {
            // Curios not installed — skip
        }

        return false;
    }

    private static boolean isMatchingPersonalAccessTerminal(ItemStack stack, BlockPos niPos) {
        if (!(stack.getItem() instanceof PersonalAccessTerminalItem)) return false;
        BlockPos linked = stack.get(Registration.WIRELESS_NI_POS.get());
        return niPos.equals(linked);
    }

    // -------------------------------------------------------------------------
    // MenuProvider inner record
    // -------------------------------------------------------------------------

    public record Provider(BlockPos niPos) implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.tremendousstorage.access_terminal");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new PersonalAccessTerminalMenu(id, inv, niPos);
        }
    }
}
