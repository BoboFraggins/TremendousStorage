package net.bobofraggins.tremendousstorage.experiencesyringe;

import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Registers the {@link IFluidHandlerItem} capability on the Experience Syringe. */
public final class ExperienceSyringeEvents {

    private ExperienceSyringeEvents() {}

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, ctx) -> new ExperienceSyringeFluidHandler(stack),
                Registration.EXPERIENCE_SYRINGE.get());
    }
}
