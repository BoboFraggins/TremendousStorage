package net.bobofraggins.intellistore.storage.items;

import net.bobofraggins.intellistore.IntelliStore;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Client-side event subscriber that registers rendering properties for the Healing Salve fluid. */
@EventBusSubscriber(modid = IntelliStore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HealingSalveClientEvents {

    private HealingSalveClientEvents() {}

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(
                new IClientFluidTypeExtensions() {
                    private static final ResourceLocation STILL =
                            ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "fluid/healing_salve_still");
                    private static final ResourceLocation FLOWING =
                            ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "fluid/healing_salve_flow");

                    @Override
                    public ResourceLocation getStillTexture() {
                        return STILL;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return FLOWING;
                    }
                },
                Registration.HEALING_SALVE_TYPE.get());
    }
}
