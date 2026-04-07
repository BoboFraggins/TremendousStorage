package net.bobofraggins.tremendousstorage.storage.personalaccessterminal;

import java.util.List;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.tremendousstorage.storage.wirelesshub.WirelessHubBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

    @Override
    public String getDescriptionId() {
        return "item.tremendousstorage.personal_access_terminal";
    }

    // -------------------------------------------------------------------------
    // Tooltip
    // -------------------------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        BlockPos niPos = stack.get(Registration.WIRELESS_NI_POS.get());
        if (niPos != null) {
            lines.add(Component.translatable(
                    "item.tremendousstorage.personal_access_terminal.linked",
                    niPos.getX(),
                    niPos.getY(),
                    niPos.getZ()));
        } else {
            lines.add(Component.translatable("item.tremendousstorage.personal_access_terminal.unlinked"));
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
            player.displayClientMessage(
                    Component.translatable("item.tremendousstorage.personal_access_terminal.not_linked"), true);
            return InteractionResultHolder.fail(stack);
        }

        BlockPos hubPos = stack.get(Registration.WIRELESS_HUB_POS.get());
        openSatUi((ServerPlayer) player, niPos, hubPos);
        return InteractionResultHolder.success(stack);
    }

    /**
     * Opens the SAT UI for the given player, connecting to the network at {@code niPos}.
     * Validates that the NI still exists, the network is valid, and — if {@code hubPos} is
     * provided — that the originating Wireless Hub is still present on that network.
     */
    public static void openSatUi(ServerPlayer player, BlockPos niPos, @Nullable BlockPos hubPos) {
        ServerLevel level = player.serverLevel();

        if (!(level.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni)) {
            player.displayClientMessage(
                    Component.translatable("item.tremendousstorage.personal_access_terminal.invalid_network"), true);
            return;
        }
        if (!ni.isNetworkValid()) {
            player.displayClientMessage(
                    Component.translatable("item.tremendousstorage.personal_access_terminal.invalid_network"), true);
            return;
        }
        if (hubPos != null) {
            if (!(level.getBlockEntity(hubPos) instanceof WirelessHubBlockEntity hub)
                    || !niPos.equals(hub.getOrFindNiPos(level))) {
                player.displayClientMessage(
                        Component.translatable("item.tremendousstorage.personal_access_terminal.hub_disconnected"),
                        true);
                return;
            }
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
