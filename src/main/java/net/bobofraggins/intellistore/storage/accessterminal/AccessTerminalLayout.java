package net.bobofraggins.intellistore.storage.accessterminal;

/** Single source of truth for all Access Terminal GUI layout constants. */
public final class AccessTerminalLayout {

    private AccessTerminalLayout() {}

    public static final int SLOT_SIZE = 18;
    public static final int BG_WIDTH = 182;
    public static final int TITLE_H = 17;

    private static final int GAP = 4;

    // ── Network grid (top section) ────────────────────────────────────────────

    public static final int NETWORK_COLS = 9;
    public static final int NETWORK_VISIBLE_ROWS = 4;
    public static final int NETWORK_X = 8;
    public static final int NETWORK_Y = TITLE_H + 1; // 18
    public static final int NETWORK_W = NETWORK_COLS * SLOT_SIZE; // 162
    public static final int NETWORK_H = NETWORK_VISIBLE_ROWS * SLOT_SIZE; // 72
    public static final int SCROLLBAR_W = 6;
    public static final int SCROLLBAR_X = NETWORK_X + NETWORK_W; // 170

    // ── Crafting section ──────────────────────────────────────────────────────

    public static final int CRAFTING_Y = NETWORK_Y + NETWORK_H + GAP; // 94
    public static final int CRAFTING_GRID_X = 30;
    public static final int CRAFTING_RESULT_X = 120;
    public static final int CRAFTING_RESULT_Y = CRAFTING_Y + SLOT_SIZE; // 112
    public static final int CRAFTING_ROWS = 3;
    public static final int CRAFTING_H = CRAFTING_ROWS * SLOT_SIZE; // 54

    // Arrow sprite from crafting_table.png (256×256 texture)
    public static final int ARROW_SRC_X = 90;
    public static final int ARROW_SRC_Y = 30;
    public static final int ARROW_W = 31;
    public static final int ARROW_H = 22;
    // Horizontally centred between grid right edge (84) and result slot left (120)
    public static final int ARROW_X =
            (CRAFTING_GRID_X + CRAFTING_ROWS * SLOT_SIZE + CRAFTING_RESULT_X - ARROW_W + 4) / 2; // 86

    // ── Player inventory ──────────────────────────────────────────────────────

    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = CRAFTING_Y + CRAFTING_H + GAP + 14; // 166
    public static final int PLAYER_INV_ROWS = 3;

    // ── Hotbar ────────────────────────────────────────────────────────────────

    public static final int HOTBAR_X = 8;
    public static final int HOTBAR_Y = PLAYER_INV_Y + PLAYER_INV_ROWS * SLOT_SIZE + GAP; // 210

    // ── Full panel ────────────────────────────────────────────────────────────

    public static final int BG_HEIGHT = HOTBAR_Y + SLOT_SIZE + GAP; // 232
}
