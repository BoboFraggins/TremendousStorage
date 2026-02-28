package net.bobofraggins.intellistore.shared.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Common (server-authoritative) configuration for IntelliStore. */
public class IntelliStoreConfig {

    public static final ModConfigSpec SPEC;

    // -------------------------------------------------------------------------
    // Power features
    // -------------------------------------------------------------------------

    /** Whether the Stirling Engine block is enabled. Defaults to {@code true}. */
    public static final ModConfigSpec.BooleanValue STIRLING_ENGINE_ENABLED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Power features").push("power");
        STIRLING_ENGINE_ENABLED = builder.comment(
                        "Enable the Stirling Engine block (heat-to-RF generator). Default: true.")
                .define("stirlingEngineEnabled", true);
        builder.pop();

        SPEC = builder.build();
    }
}
