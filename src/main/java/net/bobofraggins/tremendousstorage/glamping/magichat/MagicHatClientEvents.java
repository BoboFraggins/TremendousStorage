package net.bobofraggins.tremendousstorage.glamping.magichat;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only mod-bus events for the Magic Hat — curio renderer registration. */
public final class MagicHatClientEvents {

    private MagicHatClientEvents() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!ModList.get().isLoaded("curios")) return;
        try {
            top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(
                    Registration.MAGIC_HAT_ITEM.get(), MagicHatCurioRenderer::new);
        } catch (NoClassDefFoundError ignored) {
        }
    }
}
