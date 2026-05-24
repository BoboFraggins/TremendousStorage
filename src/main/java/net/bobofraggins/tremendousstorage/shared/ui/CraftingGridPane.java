package net.bobofraggins.tremendousstorage.shared.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Dialog pane that renders a 3×3 crafting grid, arrow, and result slot backgrounds.
 *
 * <p>Used by Tremendous Chest, Tremendous Backpack, and Storage Access Terminal screens
 * when the crafting upgrade is installed.
 */
public class CraftingGridPane implements IDialogPane {

    private static final Identifier CRAFTING_TEXTURE =
            Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");

    public static final int SLOT_SIZE = 18;
    public static final int CRAFTING_ROWS = 3;
    /** Pixel size of the clear-grid button (square). */
    public static final int BUTTON_SIZE = 10;

    /** Height of this pane: 3 rows of slots + 4 px gap before player inventory. */
    public static final int HEIGHT = CRAFTING_ROWS * SLOT_SIZE + 4;

    // Arrow sprite from crafting_table.png (256×256 texture)
    private static final int ARROW_SRC_X = 90;
    private static final int ARROW_SRC_Y = 30;
    private static final int ARROW_W = 31;
    private static final int ARROW_H = 22;
    /** Pane-local Y of the top of the arrow sprite. */
    public static final int ARROW_LOCAL_Y = (CRAFTING_ROWS * SLOT_SIZE - ARROW_H) / 2 - 3;

    // Result slot background blitted from crafting_table.png
    private static final int RESULT_BG_SRC_X = 119;
    private static final int RESULT_BG_SRC_Y = 30;
    private static final int RESULT_BG_SIZE = 26;
    private static final int RESULT_BG_OFFSET = 5;
    /** Pane-local Y of the result slot (centred vertically on the 3-row grid). */
    public static final int RESULT_LOCAL_Y = SLOT_SIZE;

    private final int craftingGridX;
    private final int resultX;
    private final int arrowX;

    /**
     * Pane-local X of the clear-grid button: 2 px to the right of the crafting grid's
     * right edge. The button is top-aligned (pane-local Y = 0).
     */
    public int clearButtonLocalX() {
        return craftingGridX + CRAFTING_ROWS * SLOT_SIZE + 2;
    }

    public CraftingGridPane() {
        this(30, 120);
    }

    public CraftingGridPane(int craftingGridX, int resultX) {
        this.craftingGridX = craftingGridX;
        this.resultX = resultX;
        this.arrowX = (craftingGridX + CRAFTING_ROWS * SLOT_SIZE + resultX - ARROW_W + 4) / 2;
    }

    @Override
    public int preferredWidth() {
        return PlayerInventoryPane.WIDTH;
    }

    @Override
    public int preferredHeight() {
        return HEIGHT;
    }

    @Override
    public void render(
            GuiGraphicsExtractor graphics, Font font, int width, int localMouseX, int localMouseY, float partialTick) {
        // 3×3 crafting grid slot backgrounds
        for (int row = 0; row < CRAFTING_ROWS; row++) {
            for (int col = 0; col < CRAFTING_ROWS; col++) {
                drawSlotBg(graphics, craftingGridX + col * SLOT_SIZE, row * SLOT_SIZE);
            }
        }

        // Arrow
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CRAFTING_TEXTURE,
                arrowX,
                ARROW_LOCAL_Y,
                ARROW_SRC_X,
                ARROW_SRC_Y,
                ARROW_W,
                ARROW_H,
                256,
                256);

        // Result slot background
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CRAFTING_TEXTURE,
                resultX - RESULT_BG_OFFSET,
                RESULT_LOCAL_Y - RESULT_BG_OFFSET,
                RESULT_BG_SRC_X,
                RESULT_BG_SRC_Y,
                RESULT_BG_SIZE,
                RESULT_BG_SIZE,
                256,
                256);
    }

    private static void drawSlotBg(GuiGraphicsExtractor graphics, int sx, int sy) {
        graphics.fill(sx, sy, sx + 16, sy + 1, 0xFF373737);
        graphics.fill(sx, sy + 1, sx + 1, sy + 16, 0xFF373737);
        graphics.fill(sx, sy + 16, sx + 17, sy + 17, 0xFFFFFFFF);
        graphics.fill(sx + 16, sy, sx + 17, sy + 16, 0xFFFFFFFF);
        graphics.fill(sx + 1, sy + 1, sx + 16, sy + 16, 0xFF8B8B8B);
    }
}
