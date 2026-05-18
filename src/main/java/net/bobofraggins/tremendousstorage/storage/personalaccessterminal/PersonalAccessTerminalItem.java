package net.bobofraggins.tremendousstorage.storage.personalaccessterminal;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.networkinterface.NetworkInterfaceBlockEntity;
import net.bobofraggins.tremendousstorage.storage.wirelesshub.WirelessHubBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
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

    public PersonalAccessTerminalItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        boolean hasCrafting = Boolean.TRUE.equals(stack.get(Registration.WIRELESS_SAT_HAS_CRAFTING_UPGRADE.get()));
        return hasCrafting ? Component.empty().append(base).append(" (Crafting)") : base;
    }

    // -------------------------------------------------------------------------
    // Tooltip
    // -------------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext ctx,
            TooltipDisplay lines,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag) {
        BlockPos niPos = stack.get(Registration.WIRELESS_NI_POS.get());
        if (niPos != null) {
            tooltipAdder.accept(Component.translatable(
                    "item.tremendousstorage.personal_access_terminal.linked",
                    niPos.getX(),
                    niPos.getY(),
                    niPos.getZ()));
        } else {
            tooltipAdder.accept(Component.translatable("item.tremendousstorage.personal_access_terminal.unlinked"));
        }
    }

    // -------------------------------------------------------------------------
    // Right-click to open SAT UI
    // -------------------------------------------------------------------------

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos niPos = stack.get(Registration.WIRELESS_NI_POS.get());
        if (niPos == null) {
            player.sendOverlayMessage(
                    Component.translatable("item.tremendousstorage.personal_access_terminal.not_linked"));
            return InteractionResult.FAIL;
        }

        BlockPos hubPos = stack.get(Registration.WIRELESS_HUB_POS.get());
        Identifier hubDimId = stack.get(Registration.WIRELESS_HUB_DIMENSION.get());
        boolean hasCraftingUpgrade =
                Boolean.TRUE.equals(stack.get(Registration.WIRELESS_SAT_HAS_CRAFTING_UPGRADE.get()));
        openSatUi((ServerPlayer) player, niPos, hubPos, hubDimId, hasCraftingUpgrade);
        return InteractionResult.SUCCESS;
    }

    /**
     * Opens the SAT UI for the given player, connecting to the network at {@code niPos}.
     *
     * <p>Validates that:
     * <ul>
     *   <li>The NI still exists and the network is valid.
     *   <li>If the hub/NI are in a different dimension than the player, the hub has the
     *       Interdimensional Upgrade applied.
     *   <li>If {@code hubPos} is provided, the hub is still present on that network.
     * </ul>
     */
    public static void openSatUi(
            ServerPlayer player,
            BlockPos niPos,
            @Nullable BlockPos hubPos,
            @Nullable Identifier hubDimensionId,
            boolean hasCraftingUpgrade) {

        // Resolve the server level where the hub and NI live
        ServerLevel hubLevel = resolveHubLevel(player, hubDimensionId);

        // Cross-dimension check: the hub must have the Interdimensional Upgrade
        if (!hubLevel.dimension().equals(player.level().dimension())) {
            boolean upgradePresent = hubPos != null
                    && hubLevel.getBlockEntity(hubPos) instanceof WirelessHubBlockEntity hub
                    && hub.hasInterdimensionalUpgrade();
            if (!upgradePresent) {
                player.sendOverlayMessage(
                        Component.translatable("item.tremendousstorage.personal_access_terminal.hub_out_of_range"));
                return;
            }
        }

        if (!(hubLevel.getBlockEntity(niPos) instanceof NetworkInterfaceBlockEntity ni)) {
            player.sendOverlayMessage(
                    Component.translatable("item.tremendousstorage.personal_access_terminal.invalid_network"));
            return;
        }
        if (!ni.isNetworkValid()) {
            player.sendOverlayMessage(
                    Component.translatable("item.tremendousstorage.personal_access_terminal.invalid_network"));
            return;
        }
        if (hubPos != null) {
            if (!(hubLevel.getBlockEntity(hubPos) instanceof WirelessHubBlockEntity hub)
                    || !niPos.equals(hub.getOrFindNiPos(hubLevel))) {
                player.sendOverlayMessage(
                        Component.translatable("item.tremendousstorage.personal_access_terminal.hub_disconnected"));
                return;
            }
        }

        // Use the NI position as both satPos and niPos — the client-side constructor reads
        // satPos first, then optionally niPos. PersonalAccessTerminalMenu.stillValid ignores satPos.
        player.openMenu(new PersonalAccessTerminalMenu.Provider(niPos, hasCraftingUpgrade, hubDimensionId), buf -> {
            buf.writeBlockPos(niPos); // satPos (ignored by PersonalAccessTerminalMenu.stillValid)
            buf.writeBoolean(true); // hasNiPos = true
            buf.writeBlockPos(niPos); // niPos
            buf.writeBoolean(hasCraftingUpgrade);
        });
    }

    /** Kept for callers that do not have a hubDimensionId (e.g. old packets on a live server). */
    public static void openSatUi(
            ServerPlayer player, BlockPos niPos, @Nullable BlockPos hubPos, boolean hasCraftingUpgrade) {
        openSatUi(player, niPos, hubPos, null, hasCraftingUpgrade);
    }

    private static ServerLevel resolveHubLevel(ServerPlayer player, @Nullable Identifier hubDimensionId) {
        if (hubDimensionId != null) {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, hubDimensionId);
            ServerLevel level = ((net.minecraft.server.level.ServerLevel) player.level())
                    .getServer()
                    .getLevel(key);
            if (level != null) return level;
        }
        return player.level();
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
