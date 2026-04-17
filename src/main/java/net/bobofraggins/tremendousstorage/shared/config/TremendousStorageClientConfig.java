package net.bobofraggins.tremendousstorage.shared.config;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-side configuration for TremendousStorage. */
public class TremendousStorageClientConfig {

    public static final ModConfigSpec SPEC;

    // -------------------------------------------------------------------------
    // Network grid sort mode
    // -------------------------------------------------------------------------

    public static final ModConfigSpec.ConfigValue<String> SORT_MODE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

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

    // -------------------------------------------------------------------------
    // Inventory grid row computation
    // -------------------------------------------------------------------------

    private static final int MARGIN = 50;
    private static final int MIN_ROWS = 2;
    private static final int MAX_ROWS = 20;
    private static final int SLOT_SIZE = 18;

    // Fixed dialog overhead in pixels, not counting network grid rows:
    //   Dialog.TITLE_H(17) + network non-row portion(5) + blank pane(20)
    //   + PlayerInventoryPane.HEIGHT(80) + Dialog.BOTTOM_PADDING(5) = 127
    // Add CraftingGridPane.HEIGHT(58) when the crafting upgrade is installed.
    private static final int FIXED_HEIGHT_BASE = 127;
    private static final int CRAFTING_GRID_HEIGHT = 58;

    /**
     * Computes the number of network grid rows that fit within the current window with a
     * {@value #MARGIN}px margin above and below. Accounts for the extra height consumed by the
     * crafting grid pane when a crafting upgrade is installed.
     *
     * <p>Must only be called from the client thread.
     */
    public static int computeVisibleRows(boolean hasCraftingUpgrade) {
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int fixed = FIXED_HEIGHT_BASE + (hasCraftingUpgrade ? CRAFTING_GRID_HEIGHT : 0);
        int rows = (screenH - 2 * MARGIN - fixed) / SLOT_SIZE;
        return Math.max(MIN_ROWS, Math.min(MAX_ROWS, rows));
    }

    /**
     * Returns the number of visible rows for screens that do not have a crafting grid.
     *
     * <p>Must only be called from the client thread.
     */
    public static int getVisibleRows() {
        return computeVisibleRows(false);
    }

    /**
     * Safe to call from shared (client + server) code. Returns the computed row count on the
     * client and the server default on the server (slot positions are unused server-side).
     */
    public static int getVisibleRowsSafe() {
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            return getVisibleRows();
        }
        return ROWS_SCALE_4_PLUS_DEFAULT;
    }

    /** Compile-time default used on the server side for slot registration. */
    public static final int ROWS_SCALE_4_PLUS_DEFAULT = 4;
}
