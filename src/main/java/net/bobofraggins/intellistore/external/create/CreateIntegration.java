package net.bobofraggins.intellistore.external.create;

import com.simibubi.create.api.registry.CreateRegistries;
import net.bobofraggins.intellistore.IntelliStore;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Registers IntelliStore's Create integration.
 *
 * <p>Only loaded when Create is present (guarded by {@code ModList.get().isLoaded("create")}
 * in {@link net.bobofraggins.intellistore.IntelliStore}).
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
                ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "healing_salve"),
                HealingSalveFanProcessingType::new);
    }
}
