package net.bobofraggins.intellistore.external.create;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import net.bobofraggins.intellistore.IntelliStore;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

/**
 * Registers IntelliStore's Create integration.
 *
 * <p>Only loaded when Create is present (guarded by {@code ModList.get().isLoaded("create")}
 * in {@link net.bobofraggins.intellistore.IntelliStore}).
 */
public final class CreateIntegration {

    /** Registered fan processing type — reference kept to prevent GC. */
    public static final HealingSalveFanProcessingType HEALING_SALVE_FAN_TYPE = Registry.register(
            CreateBuiltInRegistries.FAN_PROCESSING_TYPE,
            ResourceLocation.fromNamespaceAndPath(IntelliStore.MODID, "healing_salve"),
            new HealingSalveFanProcessingType());

    private CreateIntegration() {}

    /**
     * Triggers class loading and therefore the static registration above.
     * Call this once from the mod constructor when Create is loaded.
     */
    public static void register() {}
}
