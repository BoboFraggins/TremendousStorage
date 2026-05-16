package net.bobofraggins.tremendousstorage.storage.honey;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Registers client-side rendering for the Honey fluid. */
public final class HoneyClientEvents {

    private HoneyClientEvents() {}

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(
                new IClientFluidTypeExtensions() {
                    private static final Identifier STILL = Identifier.withDefaultNamespace("block/honey_block_top");
                    private static final Identifier FLOWING = Identifier.withDefaultNamespace("block/honey_block_side");

                    @Override
                    public Identifier getStillTexture() {
                        return STILL;
                    }

                    @Override
                    public Identifier getFlowingTexture() {
                        return FLOWING;
                    }

                    @Override
                    public Identifier getOverlayTexture() {
                        return STILL;
                    }
                },
                Registration.HONEY_TYPE.get());
    }
}
