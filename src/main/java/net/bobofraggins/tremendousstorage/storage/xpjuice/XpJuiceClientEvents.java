package net.bobofraggins.tremendousstorage.storage.xpjuice;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Registers client-side rendering for the XP Juice fluid. */
public final class XpJuiceClientEvents {

    private XpJuiceClientEvents() {}

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(
                new IClientFluidTypeExtensions() {
                    private static final Identifier STILL =
                            Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "fluid/positive_vibes_still");
                    private static final Identifier FLOWING =
                            Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "fluid/positive_vibes_flow");

                    @Override
                    public Identifier getStillTexture() {
                        return STILL;
                    }

                    @Override
                    public Identifier getFlowingTexture() {
                        return FLOWING;
                    }

                    /** Neon green tint applied over the Positive Vibes texture. */
                    @Override
                    public int getTintColor() {
                        return 0xFF39FF14;
                    }
                },
                Registration.XP_JUICE_TYPE.get());
    }
}
