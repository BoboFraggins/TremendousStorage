package net.bobofraggins.tremendousstorage.storage.personalaccessterminal;

import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.accessterminal.AccessTerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Server-side menu for the Wireless SAT item.
 *
 * <p>Uses the same menu type as {@link AccessTerminalMenu} so the client opens
 * {@link net.bobofraggins.tremendousstorage.shared.ui.AccessTerminalScreen} automatically.
 * Overrides {@link #stillValid} to check that the player still has a linked Wireless SAT
 * (in inventory, off-hand, or Curios slot) rather than checking for a physical SAT block.
 */
public class PersonalAccessTerminalMenu extends AccessTerminalMenu {

    @Nullable
    private final ResourceKey<Level> hubDimensionKey;

    /**
     * Server-side constructor.
     *
     * @param niPos              the linked Network Interface position (used as both satPos and niPos)
     * @param hasCraftingUpgrade whether the crafting upgrade is applied to this PAT
     * @param hubDimensionId     the dimension the hub (and NI) live in, or null for player's dimension
     */
    public PersonalAccessTerminalMenu(
            int id,
            Inventory inv,
            BlockPos niPos,
            boolean hasCraftingUpgrade,
            @Nullable ResourceLocation hubDimensionId) {
        // Pass niPos as both satPos and niPos — stillValid is overridden below.
        super(id, inv, niPos, niPos, hasCraftingUpgrade);
        this.hubDimensionKey = hubDimensionId != null ? ResourceKey.create(Registries.DIMENSION, hubDimensionId) : null;
    }

    public PersonalAccessTerminalMenu(int id, Inventory inv, BlockPos niPos, boolean hasCraftingUpgrade) {
        this(id, inv, niPos, hasCraftingUpgrade, null);
    }

    @Override
    protected Level getNiLevel(Player player) {
        if (hubDimensionKey != null && player instanceof ServerPlayer sp) {
            ServerLevel level = sp.getServer().getLevel(hubDimensionKey);
            if (level != null) return level;
        }
        return player.level();
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

    public record Provider(BlockPos niPos, boolean hasCraftingUpgrade, @Nullable ResourceLocation hubDimensionId)
            implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.tremendousstorage.access_terminal");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new PersonalAccessTerminalMenu(id, inv, niPos, hasCraftingUpgrade, hubDimensionId);
        }
    }
}
