package net.bobofraggins.tremendousstorage.glamping.magichat;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.world.entity.EntityType;
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
                renderer.addLayer(new MagicHatHelmetLayer<>(renderer));
            }
        }

        addHatLayerTo(event, EntityType.ARMOR_STAND, ArmorStandRenderState.class);
        addHatLayerTo(event, EntityType.ZOMBIE, ZombieRenderState.class);
    }

    @SuppressWarnings("unchecked")
    private static <
                    E extends net.minecraft.world.entity.LivingEntity,
                    S extends net.minecraft.client.renderer.entity.state.HumanoidRenderState>
            void addHatLayerTo(EntityRenderersEvent.AddLayers event, EntityType<E> type, Class<S> stateClass) {
        LivingEntityRenderer<E, S, HumanoidModel<S>> renderer =
                (LivingEntityRenderer<E, S, HumanoidModel<S>>) event.getRenderer(type);
        if (renderer != null) {
            renderer.addLayer(new MagicHatHelmetLayer<>(renderer));
        }
    }
}
