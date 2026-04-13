package net.bobofraggins.tremendousstorage.shared.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Common (server-authoritative) configuration for TremendousStorage. */
public class TremendousStorageConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue STIRLING_ENGINE_ENABLED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("power");
        STIRLING_ENGINE_ENABLED = builder.comment(
                        "Enable the Stirling Engine block (heat-to-RF generator). Default: true.")
                .define("stirlingEngineEnabled", true);
        builder.pop();
        SPEC = builder.build();
    }
}
