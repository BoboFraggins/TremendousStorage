package net.bobofraggins.tremendousstorage.storage.tremendousbackpack;

import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.network.OpenTremendousBackpackPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles the Tremendous Backpack keybind ('B') on the client.
 *
 * <p>On each client tick, checks if the keybind was pressed. If so, scans the player's main
 * inventory, off-hand, and Curios slots for a Tremendous Backpack and sends
 * {@link OpenTremendousBackpackPacket} to the server to open the UI.
 */
@EventBusSubscriber(modid = TremendousStorage.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class TremendousBackpackClientTickHandler {

    private TremendousBackpackClientTickHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        while (TremendousBackpackClientEvents.OPEN_TREMENDOUS_BACKPACK != null
                && TremendousBackpackClientEvents.OPEN_TREMENDOUS_BACKPACK.consumeClick()) {
            OpenTremendousBackpackPacket packet = findBackpack(mc);
            if (packet != null) {
                PacketDistributor.sendToServer(packet);
            }
        }
    }

    /**
     * Scans the player's main inventory (slots 0-35), off-hand (slot 40), and Curios slots for a
     * Tremendous Backpack. Returns a ready-to-send packet for the first one found, or {@code null}.
     */
    @Nullable
    private static OpenTremendousBackpackPacket findBackpack(Minecraft mc) {
        // Main inventory (slots 0-35)
        var items = mc.player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            if (isBackpack(items.get(i))) {
                return new OpenTremendousBackpackPacket(OpenTremendousBackpackPacket.SLOT_INVENTORY, i, "");
            }
        }
        // Off-hand (slot 40)
        if (isBackpack(mc.player.getOffhandItem())) {
            return new OpenTremendousBackpackPacket(OpenTremendousBackpackPacket.SLOT_INVENTORY, 40, "");
        }

        // Curios slots (soft dependency)
        try {
            var curiosInv = mc.player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
            if (curiosInv != null) {
                for (var entry : curiosInv.getCurios().entrySet()) {
                    String slotId = entry.getKey();
                    var handler = entry.getValue().getStacks();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        if (isBackpack(handler.getStackInSlot(i))) {
                            return new OpenTremendousBackpackPacket(
                                    OpenTremendousBackpackPacket.SLOT_CURIOS, i, slotId);
                        }
                    }
                }
            }
        } catch (NoClassDefFoundError | Exception ignored) {
            // Curios not installed
        }

        return null;
    }

    private static boolean isBackpack(ItemStack stack) {
        return stack.getItem() instanceof TremendousBackpackItem;
    }
}
