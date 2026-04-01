package net.bobofraggins.intellistore.shared.config;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-side configuration for IntelliStore. */
public class IntelliStoreClientConfig {

    public static final ModConfigSpec SPEC;

    // -------------------------------------------------------------------------
    // Inventory grid rows by GUI scale
    // -------------------------------------------------------------------------

    public static final ModConfigSpec.IntValue ROWS_SCALE_1;
    public static final ModConfigSpec.IntValue ROWS_SCALE_2;
    public static final ModConfigSpec.IntValue ROWS_SCALE_3;
    public static final ModConfigSpec.IntValue ROWS_SCALE_4_PLUS;

    // -------------------------------------------------------------------------
    // Network grid sort mode
    // -------------------------------------------------------------------------

    public static final ModConfigSpec.ConfigValue<String> SORT_MODE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Number of item grid rows shown in storage screens, by GUI scale.")
                .push("inventory_rows");
        ROWS_SCALE_1 = builder.comment("Rows at GUI scale 1.").defineInRange("scale1", 20, 1, 50);
        ROWS_SCALE_2 = builder.comment("Rows at GUI scale 2.").defineInRange("scale2", 15, 1, 50);
        ROWS_SCALE_3 = builder.comment("Rows at GUI scale 3.").defineInRange("scale3", 10, 1, 50);
        ROWS_SCALE_4_PLUS = builder.comment("Rows at GUI scale 4 and above.").defineInRange("scale4plus", 4, 1, 50);
        builder.pop();

        SORT_MODE = builder.comment("Sort mode for the network item grid. One of: AMOUNT, NAME, MOD.")
                .define("sort_mode", SortMode.AMOUNT.name());

        SPEC = builder.build();
    }

    public static SortMode getSortMode() {
        try {
            return SortMode.valueOf(SORT_MODE.get());
        } catch (IllegalArgumentException e) {
            return SortMode.AMOUNT;
        }
    }

    public static void setSortMode(SortMode mode) {
        SORT_MODE.set(mode.name());
    }

    /**
     * Returns the configured number of inventory rows for the current GUI scale.
     *
     * <p>Must only be called from the client thread (reads {@link Minecraft#getInstance()}).
     */
    public static int getVisibleRows() {
        int scale = (int) Minecraft.getInstance().getWindow().getGuiScale();
        if (scale <= 1) return ROWS_SCALE_1.get();
        if (scale == 2) return ROWS_SCALE_2.get();
        if (scale == 3) return ROWS_SCALE_3.get();
        return ROWS_SCALE_4_PLUS.get();
    }

    /**
     * Safe to call from shared (client + server) code. Returns the configured row count on the
     * client and the scale-4+ default on the server (slot positions are unused server-side).
     */
    public static int getVisibleRowsSafe() {
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            return getVisibleRows();
        }
        return ROWS_SCALE_4_PLUS_DEFAULT;
    }

    /** Compile-time default used on the server (must match {@link #ROWS_SCALE_4_PLUS} default). */
    public static final int ROWS_SCALE_4_PLUS_DEFAULT = 4;
}
