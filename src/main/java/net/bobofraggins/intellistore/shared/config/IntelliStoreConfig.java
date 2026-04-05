package net.bobofraggins.intellistore.shared.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Common (server-authoritative) configuration for IntelliStore. */
public class IntelliStoreConfig {

    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        SPEC = builder.build();
    }
}
