package net.bobofraggins.tremendousstorage.experiencesyringe;

import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.util.LegacyFluidHandlerWrapper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ExperienceSyringeEvents {

    private ExperienceSyringeEvents() {}

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
                Capabilities.Fluid.ITEM,
                (stack, ctx) -> new LegacyFluidHandlerWrapper(new ExperienceSyringeFluidHandler(stack)),
                Registration.EXPERIENCE_SYRINGE.get());
    }
}
