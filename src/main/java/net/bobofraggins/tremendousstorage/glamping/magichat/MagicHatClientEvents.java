package net.bobofraggins.tremendousstorage.glamping.magichat;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Client-only mod-bus events for the Magic Hat — curio renderer registration. */
public final class MagicHatClientEvents {

    private MagicHatClientEvents() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ICurioRenderer.register(Registration.MAGIC_HAT_ITEM.get(), MagicHatCurioRenderer::new);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType skin : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skin);
            if (renderer != null) {
                renderer.addLayer(new MagicHatHelmetLayer(renderer));
            }
        }
    }
}
