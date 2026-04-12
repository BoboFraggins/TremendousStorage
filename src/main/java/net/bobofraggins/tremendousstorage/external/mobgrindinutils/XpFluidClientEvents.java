package net.bobofraggins.tremendousstorage.external.mobgrindinutils;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Registers client-side rendering for the fallback XP Juice fluid.
 *
 * <p>Does nothing when Mob Grinding Utils is loaded — MGU provides its own fluid rendering.
 * Uses the Positive Vibes flow texture recolored to neon green via {@link #getTintColor()}.
 */
public final class XpFluidClientEvents {

    private XpFluidClientEvents() {}

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        if (ModList.get().isLoaded("mob_grinding_utils")) return;

        event.registerFluidType(
                new IClientFluidTypeExtensions() {
                    private static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath(
                            TremendousStorage.MODID, "fluid/positive_vibes_still");
                    private static final ResourceLocation FLOWING =
                            ResourceLocation.fromNamespaceAndPath(TremendousStorage.MODID, "fluid/positive_vibes_flow");

                    @Override
                    public ResourceLocation getStillTexture() {
                        return STILL;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return FLOWING;
                    }

                    /** Neon green tint applied over the Positive Vibes texture. */
                    @Override
                    public int getTintColor() {
                        return 0xFF39FF14;
                    }
                },
                MobGrindingUtilsIntegration.XP_FLUID_TYPE.get());
    }
}
