package net.bobofraggins.tremendousstorage.storage.items;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Client-side event subscriber that registers rendering properties for the Positive Vibes fluid. */
public final class PositiveVibesClientEvents {

    private static final Identifier STILL =
            Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "fluid/positive_vibes_still");
    private static final Identifier FLOW =
            Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "fluid/positive_vibes_flow");

    private PositiveVibesClientEvents() {}

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {}, Registration.POSITIVE_VIBES_TYPE.get());
    }

    @SubscribeEvent
    public static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        var unbaked = new FluidModel.Unbaked(
                new Material(STILL),
                new Material(FLOW),
                new Material(STILL), // overlay shown when submerged
                null); // no tint — use texture colour as-is
        event.register(unbaked, Registration.POSITIVE_VIBES_SOURCE.get(), Registration.POSITIVE_VIBES_FLOWING.get());
    }
}
