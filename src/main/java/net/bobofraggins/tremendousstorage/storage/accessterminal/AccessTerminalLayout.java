package net.bobofraggins.tremendousstorage.storage.accessterminal;

/** Single source of truth for all Access Terminal GUI layout constants. */
public final class AccessTerminalLayout {

    private AccessTerminalLayout() {}

    public static final int SLOT_SIZE = 18;
    public static final int BG_WIDTH = 202;
    public static final int TITLE_H = 17;

    private static final int GAP = 4;

    // ── Network grid (top section) ────────────────────────────────────────────

    public static final int NETWORK_COLS = 9;
    public static final int NETWORK_VISIBLE_ROWS = 4;
    public static final int NETWORK_X = 8;
    public static final int NETWORK_Y = TITLE_H + 1; // 18
    public static final int NETWORK_W = NETWORK_COLS * SLOT_SIZE; // 162
    public static final int NETWORK_H = NETWORK_VISIBLE_ROWS * SLOT_SIZE; // 72

    /**
     * Left edge of the scrollbar separator — 8 px right of the grid, matching the 8 px left
     * margin so both sides of the scrollbar have equal padding.
     */
    public static final int SCROLLBAR_X = NETWORK_X + NETWORK_W + 9; // 179

    /** Width of the scrollbar track (matches vanilla creative inventory). */
    public static final int SCROLLBAR_W = 14;

    /** Left edge of the track (1 px right of the separator line). */
    public static final int SCROLLBAR_TRACK_X = SCROLLBAR_X + 1; // 180

    /** Pixel dimensions of the vanilla scroller thumb sprite. */
    public static final int SCROLLER_W = 12;

    public static final int SCROLLER_H = 15;

    // ── Crafting section ──────────────────────────────────────────────────────

    public static final int CRAFTING_Y = NETWORK_Y + NETWORK_H + GAP; // 94
    public static final int CRAFTING_GRID_X = 33;
    public static final int CRAFTING_RESULT_X = 123;
    public static final int CRAFTING_RESULT_Y = CRAFTING_Y + SLOT_SIZE; // 112
    public static final int CRAFTING_ROWS = 3;
    public static final int CRAFTING_H = CRAFTING_ROWS * SLOT_SIZE; // 54

    // Arrow sprite from crafting_table.png (256×256 texture)
    public static final int ARROW_SRC_X = 90;
    public static final int ARROW_SRC_Y = 30;
    public static final int ARROW_W = 31;
    public static final int ARROW_H = 22;
    // Horizontally centred between grid right edge and result slot left
    public static final int ARROW_X =
            (CRAFTING_GRID_X + CRAFTING_ROWS * SLOT_SIZE + CRAFTING_RESULT_X - ARROW_W + 4) / 2; // 91

    // ── Player inventory ──────────────────────────────────────────────────────

    /** Left margin for the player inventory, centred in BG_WIDTH: (202 - 9×18) / 2 = 20. */
    public static final int PLAYER_INV_X = 20;

    public static final int PLAYER_INV_Y = CRAFTING_Y + CRAFTING_H + GAP; // 152
    public static final int PLAYER_INV_ROWS = 3;

    // ── Hotbar ────────────────────────────────────────────────────────────────

    public static final int HOTBAR_X = 20;
    public static final int HOTBAR_Y = PLAYER_INV_Y + PLAYER_INV_ROWS * SLOT_SIZE + GAP; // 210

    // ── Full panel ────────────────────────────────────────────────────────────

    public static final int BG_HEIGHT = HOTBAR_Y + SLOT_SIZE + GAP; // 232
}
