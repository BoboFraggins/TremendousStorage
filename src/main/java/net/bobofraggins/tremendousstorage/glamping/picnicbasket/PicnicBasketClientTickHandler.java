package net.bobofraggins.tremendousstorage.glamping.picnicbasket;

import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Handles the Picnic Basket keybind (unbound by default) on the client.
 *
 * <p>On each client tick, checks if the keybind was pressed. If so, scans the player's main
 * inventory, off-hand, and Curios slots for a Picnic Basket and sends
 * {@link OpenPicnicBasketPacket} to the server to open the UI.
 */
public final class PicnicBasketClientTickHandler {

    private PicnicBasketClientTickHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        while (PicnicBasketClientEvents.OPEN_PICNIC_BASKET != null
                && PicnicBasketClientEvents.OPEN_PICNIC_BASKET.consumeClick()) {
            OpenPicnicBasketPacket packet = findBasket(mc);
            if (packet != null) {
                ClientPacketDistributor.sendToServer(packet);
            }
        }
    }

    /**
     * Scans the player's main inventory (slots 0-35), off-hand (slot 40), and Curios slots for a
     * Picnic Basket. Returns a ready-to-send packet for the first one found, or {@code null}.
     */
    @Nullable
    private static OpenPicnicBasketPacket findBasket(Minecraft mc) {
        // Main inventory (slots 0-35)
        var items = mc.player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < items.size(); i++) {
            if (isBasket(items.get(i))) {
                return new OpenPicnicBasketPacket(PicnicBasketItemUtils.SLOT_INVENTORY, i, "");
            }
        }
        // Off-hand (slot 40)
        if (isBasket(mc.player.getOffhandItem())) {
            return new OpenPicnicBasketPacket(PicnicBasketItemUtils.SLOT_INVENTORY, 40, "");
        }

        // Curios slots (soft dependency)
        try {
            var curiosInv = mc.player.getCapability(top.theillusivec4.curios.api.CuriosCapability.INVENTORY);
            if (curiosInv != null) {
                for (var entry : curiosInv.getCurios().entrySet()) {
                    String slotId = entry.getKey();
                    var handler = entry.getValue().getStacks();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        if (isBasket(handler.getStackInSlot(i))) {
                            return new OpenPicnicBasketPacket(PicnicBasketItemUtils.SLOT_CURIOS, i, slotId);
                        }
                    }
                }
            }
        } catch (NoClassDefFoundError | Exception ignored) {
            // Curios not installed
        }

        return null;
    }

    private static boolean isBasket(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() == Registration.PICNIC_BASKET_ITEM.get()
                || stack.getItem() == Registration.ENDER_PICNIC_BASKET_ITEM.get();
    }
}
