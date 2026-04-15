package net.bobofraggins.tremendousstorage.shared.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Common (server-authoritative) configuration for TremendousStorage. */
public class TremendousStorageConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue STIRLING_ENGINE_ENABLED;
    public static final ModConfigSpec.DoubleValue TIER_UPGRADE_POWER_MULT;
    public static final ModConfigSpec.DoubleValue HAARP_UPGRADE_POWER_MULT;
    public static final ModConfigSpec.DoubleValue INTERDIMENSIONAL_POWER_MULT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("power");
        STIRLING_ENGINE_ENABLED = builder.comment(
                        "Enable the Stirling Engine block (heat-to-RF generator). Default: true.")
                .define("stirlingEngineEnabled", true);
        TIER_UPGRADE_POWER_MULT = builder.comment("Power cost multiplier applied per tier upgrade on network devices"
                        + " (Network Interface, Wireless Hub). Compounded per tier above WOOD."
                        + " Default: 1.25")
                .defineInRange("tierUpgradePowerMultiplier", 1.25, 1.0, 10.0);
        HAARP_UPGRADE_POWER_MULT = builder.comment(
                        "Additional power cost multiplier applied to a Wireless Hub with the HAARP"
                                + " Upgrade installed. Default: 1.25")
                .defineInRange("haarpUpgradePowerMultiplier", 1.25, 1.0, 10.0);
        INTERDIMENSIONAL_POWER_MULT = builder.comment(
                        "Additional power cost multiplier applied to a Wireless Hub with the"
                                + " Interdimensional Upgrade installed. Default: 1.25")
                .defineInRange("interdimensionalUpgradePowerMultiplier", 1.25, 1.0, 10.0);
        builder.pop();
        SPEC = builder.build();
    }
}
