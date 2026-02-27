package net.bobofraggins.intellistore.wirelesssat;

import javax.annotation.Nullable;
import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.network.OpenWirelessSatPacket;
import net.bobofraggins.intellistore.register.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles the Wireless SAT keybind on the client.
 *
 * <p>On each client tick, checks if the keybind was pressed. If so, scans the player's
 * main inventory and Curios slots for a linked Wireless SAT and sends
 * {@link OpenWirelessSatPacket} to the server to open the SAT UI.
 */
@EventBusSubscriber(modid = IntelliStore.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class WirelessSatClientTickHandler {

    private WirelessSatClientTickHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        while (WirelessSatClientEvents.OPEN_WIRELESS_SAT != null
                && WirelessSatClientEvents.OPEN_WIRELESS_SAT.consumeClick()) {
            BlockPos niPos = findLinkedWirelessSat(mc);
            if (niPos != null) {
                PacketDistributor.sendToServer(new OpenWirelessSatPacket(niPos));
            }
        }
    }

    /**
     * Scans the player's main inventory, off-hand, and Curios slots for a linked Wireless SAT.
     * Returns the NI position of the first one found, or {@code null} if none.
     */
    @Nullable
    private static BlockPos findLinkedWirelessSat(Minecraft mc) {
        // Main inventory + off-hand
        for (ItemStack stack : mc.player.getInventory().items) {
            BlockPos pos = getLinkedPos(stack);
            if (pos != null) return pos;
        }
        BlockPos offhand = getLinkedPos(mc.player.getOffhandItem());
        if (offhand != null) return offhand;

        // Curios slots (soft dependency)
        try {
            var curiosInv = mc.player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
            if (curiosInv != null) {
                for (var entry : curiosInv.getCurios().entrySet()) {
                    var handler = entry.getValue().getStacks();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        BlockPos pos = getLinkedPos(handler.getStackInSlot(i));
                        if (pos != null) return pos;
                    }
                }
            }
        } catch (NoClassDefFoundError | Exception ignored) {
            // Curios not installed
        }

        return null;
    }

    @Nullable
    private static BlockPos getLinkedPos(ItemStack stack) {
        if (!(stack.getItem() instanceof WirelessSatItem)) return null;
        return stack.get(Registration.WIRELESS_NI_POS.get());
    }
}
