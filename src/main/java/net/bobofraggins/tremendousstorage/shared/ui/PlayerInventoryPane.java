package net.bobofraggins.tremendousstorage.shared.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Dialog pane that renders the player inventory and hotbar slot backgrounds.
 *
 * <p>Local coordinate origin (0, 0) sits at the top-left of the first inventory row.
 * Vanilla {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen} renders
 * the actual item stacks on top of these backgrounds.
 *
 * <p>Can be used inside a {@link Dialog} or called directly from a screen's {@code renderBg}
 * by translating the pose stack to the player inventory origin first.
 */
public class PlayerInventoryPane implements IDialogPane {

    // Slot dimensions
    private static final int SLOT_SIZE = 18;
    private static final int COLS = 9;
    private static final int INV_ROWS = 3;

    private static final int INV_Y = 0;
    private static final int HOTBAR_GAP = 4;

    private final int invX;
    private final int hotbarX;
    private final int hotbarY;

    // Slot border colours
    private static final int DARK = 0xFF373737;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int FILL = 0xFF8B8B8B;

    /** Standard width covering all 9 columns plus left margin. */
    public static final int WIDTH = 176;

    /** Height covering 3 inventory rows + gap + hotbar row + bottom gap. */
    public static final int HEIGHT = INV_ROWS * SLOT_SIZE + HOTBAR_GAP + SLOT_SIZE + HOTBAR_GAP; // 80

    public PlayerInventoryPane() {
        this(8);
    }

    public PlayerInventoryPane(int leftMargin) {
        this.invX = leftMargin;
        this.hotbarX = leftMargin;
        this.hotbarY = INV_ROWS * SLOT_SIZE + HOTBAR_GAP;
    }

    @Override
    public int preferredWidth() {
        return WIDTH;
    }

    @Override
    public int preferredHeight() {
        return HEIGHT;
    }

    @Override
    public void render(
            GuiGraphics graphics, Font font, int width, int localMouseX, int localMouseY, float partialTick) {
        // 3×9 main inventory rows
        for (int row = 0; row < INV_ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                drawSlotBg(graphics, invX + col * SLOT_SIZE, INV_Y + row * SLOT_SIZE);
            }
        }

        // 1×9 hotbar row
        for (int col = 0; col < COLS; col++) {
            drawSlotBg(graphics, hotbarX + col * SLOT_SIZE, hotbarY);
        }
    }

    private static void drawSlotBg(GuiGraphics graphics, int sx, int sy) {
        graphics.fill(sx, sy, sx + 16, sy + 1, DARK); // top
        graphics.fill(sx, sy + 1, sx + 1, sy + 16, DARK); // left
        graphics.fill(sx, sy + 16, sx + 17, sy + 17, LIGHT); // bottom
        graphics.fill(sx + 16, sy, sx + 17, sy + 16, LIGHT); // right
        graphics.fill(sx + 1, sy + 1, sx + 16, sy + 16, FILL); // interior
    }
}
