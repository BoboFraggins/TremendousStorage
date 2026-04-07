package net.bobofraggins.tremendousstorage.shared.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Common (server-authoritative) configuration for TremendousStorage. */
public class TremendousStorageConfig {

    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        SPEC = builder.build();
    }
}
