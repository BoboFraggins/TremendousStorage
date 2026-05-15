package net.bobofraggins.tremendousstorage.storage.personalaccessterminal;

import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.network.OpenPersonalAccessTerminalPacket;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Handles the Wireless SAT keybind on the client.
 *
 * <p>On each client tick, checks if the keybind was pressed. If so, scans the player's
 * main inventory and Curios slots for a linked Wireless SAT and sends
 * {@link OpenPersonalAccessTerminalPacket} to the server to open the SAT UI.
 */
public final class PersonalAccessTerminalClientTickHandler {

    private PersonalAccessTerminalClientTickHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        while (PersonalAccessTerminalClientEvents.OPEN_WIRELESS_SAT != null
                && PersonalAccessTerminalClientEvents.OPEN_WIRELESS_SAT.consumeClick()) {
            OpenPersonalAccessTerminalPacket packet = findLinkedPersonalAccessTerminal(mc);
            if (packet != null) {
                ClientPacketDistributor.sendToServer(packet);
            }
        }
    }

    /**
     * Scans the player's main inventory, off-hand, and Curios slots for a linked Wireless SAT.
     * Returns a ready-to-send packet for the first one found, or {@code null} if none.
     */
    @Nullable
    private static OpenPersonalAccessTerminalPacket findLinkedPersonalAccessTerminal(Minecraft mc) {
        // Main inventory + off-hand
        for (ItemStack stack : mc.player.getInventory().getNonEquipmentItems()) {
            OpenPersonalAccessTerminalPacket packet = buildPacket(stack);
            if (packet != null) return packet;
        }
        OpenPersonalAccessTerminalPacket offhand = buildPacket(mc.player.getOffhandItem());
        if (offhand != null) return offhand;

        // Curios slots (soft dependency)
        try {
            var curiosInv = mc.player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
            if (curiosInv != null) {
                for (var entry : curiosInv.getCurios().entrySet()) {
                    var handler = entry.getValue().getStacks();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        OpenPersonalAccessTerminalPacket packet = buildPacket(handler.getStackInSlot(i));
                        if (packet != null) return packet;
                    }
                }
            }
        } catch (NoClassDefFoundError | Exception ignored) {
            // Curios not installed
        }

        return null;
    }

    @Nullable
    private static OpenPersonalAccessTerminalPacket buildPacket(ItemStack stack) {
        if (!(stack.getItem() instanceof PersonalAccessTerminalItem)) return null;
        BlockPos niPos = stack.get(Registration.WIRELESS_NI_POS.get());
        if (niPos == null) return null;
        BlockPos hubPos = stack.get(Registration.WIRELESS_HUB_POS.get());
        net.minecraft.resources.ResourceLocation hubDimId = stack.get(Registration.WIRELESS_HUB_DIMENSION.get());
        boolean hasCraftingUpgrade =
                Boolean.TRUE.equals(stack.get(Registration.WIRELESS_SAT_HAS_CRAFTING_UPGRADE.get()));
        return new OpenPersonalAccessTerminalPacket(niPos, hubPos, hubDimId, hasCraftingUpgrade);
    }
}
