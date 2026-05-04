package net.bobofraggins.tremendousstorage.glamping.magichat;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

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

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer pr) {
                pr.addLayer(new MagicHatHelmetLayer(pr));
            }
        }
    }
}
