package net.bobofraggins.intellistore.personalfilingcabinet;

import net.bobofraggins.intellistore.register.Registration;
import net.bobofraggins.intellistore.ui.PersonalFilingCabinetMenu;
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
        player.openMenu(new PersonalFilingCabinetMenu.Provider(pfcSlot), buf -> buf.writeInt(pfcSlot));
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
}
