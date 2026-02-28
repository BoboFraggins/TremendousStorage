package net.bobofraggins.intellistore.storage.personalfilingcabinet;

import javax.annotation.Nullable;
import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.shared.network.OpenPersonalFilingCabinetPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles the Personal Filing Cabinet keybind (Ctrl+P) on the client.
 *
 * <p>On each client tick, checks if the keybind was pressed (Ctrl+P modifier is enforced
 * by {@link KeyModifier#CONTROL} on the KeyMapping itself). Scans the player's main
 * inventory, off-hand, and Curios slots for a PFC and sends
 * {@link OpenPersonalFilingCabinetPacket} to the server to open the UI.
 */
@EventBusSubscriber(modid = IntelliStore.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class PersonalFilingCabinetClientTickHandler {

    private PersonalFilingCabinetClientTickHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        while (PersonalFilingCabinetClientEvents.OPEN_PERSONAL_FILING_CABINET != null
                && PersonalFilingCabinetClientEvents.OPEN_PERSONAL_FILING_CABINET.consumeClick()) {
            OpenPersonalFilingCabinetPacket packet = findPfc(mc);
            if (packet != null) {
                PacketDistributor.sendToServer(packet);
            }
        }
    }

    /**
     * Scans inventory then Curios slots for a Personal Filing Cabinet.
     * Returns a packet describing where it was found, or {@code null} if none.
     */
    @Nullable
    private static OpenPersonalFilingCabinetPacket findPfc(Minecraft mc) {
        // Main inventory (slots 0–35) and off-hand (slot 40)
        var items = mc.player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            if (isPfc(items.get(i))) {
                return new OpenPersonalFilingCabinetPacket(OpenPersonalFilingCabinetPacket.SLOT_INVENTORY, i, "");
            }
        }
        if (isPfc(mc.player.getOffhandItem())) {
            return new OpenPersonalFilingCabinetPacket(OpenPersonalFilingCabinetPacket.SLOT_INVENTORY, 40, "");
        }

        // Curios slots (soft dependency)
        try {
            var curiosInv = mc.player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
            if (curiosInv != null) {
                for (var entry : curiosInv.getCurios().entrySet()) {
                    String slotId = entry.getKey();
                    var handler = entry.getValue().getStacks();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        if (isPfc(handler.getStackInSlot(i))) {
                            return new OpenPersonalFilingCabinetPacket(
                                    OpenPersonalFilingCabinetPacket.SLOT_CURIOS, i, slotId);
                        }
                    }
                }
            }
        } catch (NoClassDefFoundError | Exception ignored) {
            // Curios not installed
        }

        return null;
    }

    private static boolean isPfc(ItemStack stack) {
        return stack.getItem() instanceof PersonalFilingCabinetItem;
    }
}
