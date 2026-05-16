package net.bobofraggins.tremendousstorage.storage.honey;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Registers client-side rendering for the Honey fluid. */
public final class HoneyClientEvents {

    private static final Identifier STILL = Identifier.withDefaultNamespace("block/honey_block_top");
    private static final Identifier FLOW = Identifier.withDefaultNamespace("block/honey_block_side");

    private HoneyClientEvents() {}

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {}, Registration.HONEY_TYPE.get());
    }

    @SubscribeEvent
    public static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        var unbaked = new FluidModel.Unbaked(
                new Material(STILL),
                new Material(FLOW),
                new Material(STILL), // overlay shown when submerged
                null); // no tint — honey uses its natural texture colour
        event.register(unbaked, Registration.HONEY_SOURCE.get(), Registration.HONEY_FLOWING.get());
    }
}
