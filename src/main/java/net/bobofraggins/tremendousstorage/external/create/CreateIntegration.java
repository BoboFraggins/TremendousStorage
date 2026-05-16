package net.bobofraggins.tremendousstorage.external.create;

import com.simibubi.create.api.registry.CreateRegistries;
import net.bobofraggins.tremendousstorage.TremendousStorage;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Registers TremendousStorage's Create integration.
 *
 * <p>Only loaded when Create is present (guarded by {@code ModList.get().isLoaded("create")}
 * in {@link net.bobofraggins.tremendousstorage.TremendousStorage}).
 */
public final class CreateIntegration {

    private CreateIntegration() {}

    /**
     * Subscribes to {@link RegisterEvent} so the fan processing type is registered while
     * Create's registry is still open. Call this once from the mod constructor when Create is
     * loaded.
     */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CreateIntegration::onRegister);
    }

    private static void onRegister(RegisterEvent event) {
        event.register(
                CreateRegistries.FAN_PROCESSING_TYPE,
                Identifier.fromNamespaceAndPath(TremendousStorage.MODID, "positive_vibes"),
                PositiveVibesFanProcessingType::new);
    }
}
