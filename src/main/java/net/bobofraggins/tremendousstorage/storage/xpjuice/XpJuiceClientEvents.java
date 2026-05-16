package net.bobofraggins.tremendousstorage.storage.xpjuice;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;

/** Registers client-side rendering for the XP Juice fluid. */
public final class XpJuiceClientEvents {

    private static final Identifier STILL =
            Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "fluid/positive_vibes_still");
    private static final Identifier FLOW =
            Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "fluid/positive_vibes_flow");

    private XpJuiceClientEvents() {}

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {}, Registration.XP_JUICE_TYPE.get());
    }

    @SubscribeEvent
    public static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        var unbaked = new FluidModel.Unbaked(
                new Material(STILL),
                new Material(FLOW),
                null, // no submerged overlay
                FluidTintSources.constant(0xFF39FF14)); // neon green tint
        event.register(unbaked, Registration.XP_JUICE_SOURCE.get(), Registration.XP_JUICE_FLOWING.get());
    }
}
