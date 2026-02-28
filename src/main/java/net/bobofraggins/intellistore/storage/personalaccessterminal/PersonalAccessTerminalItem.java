package net.bobofraggins.intellistore.storage.personalaccessterminal;

import java.util.List;
import javax.annotation.Nullable;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The Wireless Storage Access Terminal item.
 *
 * <p>When linked (holds a {@link Registration#WIRELESS_NI_POS} component), right-clicking
 * opens the full SAT UI connected to the stored Network Interface position.
 *
 * <p>Linking is done via the Wireless Hub block — place the item in the hub's left slot
 * and retrieve it from the right slot once the hub records the NI position.
 */
public class PersonalAccessTerminalItem extends Item {

    public PersonalAccessTerminalItem() {
        super(new Item.Properties().stacksTo(1));
    }

    // -------------------------------------------------------------------------
    // Tooltip
    // -------------------------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        BlockPos niPos = stack.get(Registration.WIRELESS_NI_POS.get());
        if (niPos != null) {
            lines.add(Component.translatable(
                    "item.intellistore.wireless_sat.linked", niPos.getX(), niPos.getY(), niPos.getZ()));
        } else {
            lines.add(Component.translatable("item.intellistore.wireless_sat.unlinked"));
        }
    }

    // -------------------------------------------------------------------------
    // Right-click to open SAT UI
    // -------------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        BlockPos niPos = stack.get(Registration.WIRELESS_NI_POS.get());
        if (niPos == null) {
            player.displayClientMessage(Component.translatable("item.intellistore.wireless_sat.not_linked"), true);
            return InteractionResultHolder.fail(stack);
        }

        openSatUi((ServerPlayer) player, niPos);
        return InteractionResultHolder.success(stack);
    }

    /**
     * Opens the SAT UI for the given player, connecting to the network at {@code niPos}.
     * Validates that the NI still exists and the network is valid before opening.
     */
    public static void openSatUi(ServerPlayer player, BlockPos niPos) {
        if (!(player.level().getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni)) {
            player.displayClientMessage(Component.translatable("item.intellistore.wireless_sat.invalid_network"), true);
            return;
        }
        if (!ni.isNetworkValid()) {
            player.displayClientMessage(Component.translatable("item.intellistore.wireless_sat.invalid_network"), true);
            return;
        }
        if (!ni.isPowered()) {
            player.displayClientMessage(Component.translatable("screen.intellistore.not_enough_power"), true);
            return;
        }

        // Use the NI position as both satPos and niPos — the client-side constructor reads
        // satPos first, then optionally niPos. PersonalAccessTerminalMenu.stillValid ignores satPos.
        player.openMenu(new PersonalAccessTerminalMenu.Provider(niPos), buf -> {
            buf.writeBlockPos(niPos); // satPos (ignored by PersonalAccessTerminalMenu.stillValid)
            buf.writeBoolean(true); // hasNiPos = true
            buf.writeBlockPos(niPos); // niPos
        });
    }

    /** Returns true if this stack has a linked NI position. */
    public static boolean isLinked(ItemStack stack) {
        return stack.has(Registration.WIRELESS_NI_POS.get());
    }

    /** Returns the linked NI position, or null if not linked. */
    @Nullable
    public static BlockPos getLinkedNiPos(ItemStack stack) {
        return stack.get(Registration.WIRELESS_NI_POS.get());
    }
}
