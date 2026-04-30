package net.bobofraggins.tremendousstorage.glamping.dankfannypack;

import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.network.OpenFannyPackPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles the Dank Fanny Pack keybind (Alt-D) on the client.
 *
 * <p>Each client tick, checks whether the keybind was pressed. If so, scans the player's main
 * inventory, off-hand, and Curios slots for a Dank Fanny Pack and sends
 * {@link OpenFannyPackPacket} to the server.
 */
public final class DankFannyPackClientTickHandler {

    private DankFannyPackClientTickHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        while (DankFannyPackClientEvents.OPEN_DANK_FANNY_PACK != null
                && DankFannyPackClientEvents.OPEN_DANK_FANNY_PACK.consumeClick()) {
            OpenFannyPackPacket packet = findFannyPack(mc);
            if (packet != null) PacketDistributor.sendToServer(packet);
        }
    }

    @Nullable
    private static OpenFannyPackPacket findFannyPack(Minecraft mc) {
        // Main inventory (slots 0-35)
        var items = mc.player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getItem() instanceof DankFannyPackItem) {
                return new OpenFannyPackPacket(OpenFannyPackPacket.SLOT_INVENTORY, i, "");
            }
        }
        // Off-hand (slot 40)
        if (mc.player.getOffhandItem().getItem() instanceof DankFannyPackItem) {
            return new OpenFannyPackPacket(OpenFannyPackPacket.SLOT_INVENTORY, 40, "");
        }
        // Curios slots (soft dependency)
        try {
            var curiosInv = mc.player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
            if (curiosInv != null) {
                for (var entry : curiosInv.getCurios().entrySet()) {
                    String slotId = entry.getKey();
                    var handler = entry.getValue().getStacks();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        if (handler.getStackInSlot(i).getItem() instanceof DankFannyPackItem) {
                            return new OpenFannyPackPacket(OpenFannyPackPacket.SLOT_CURIOS, i, slotId);
                        }
                    }
                }
            }
        } catch (NoClassDefFoundError | Exception ignored) {
        }
        return null;
    }
}
