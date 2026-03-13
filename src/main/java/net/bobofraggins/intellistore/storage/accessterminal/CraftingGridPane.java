package net.bobofraggins.intellistore.storage.accessterminal;

import net.bobofraggins.intellistore.shared.ui.IDialogPane;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Dialog pane that renders the 3×3 crafting grid, arrow, and result slot backgrounds.
 *
 * <p>Local coordinate origin (0, 0) sits at {@code topPos + AccessTerminalLayout.CRAFTING_Y}.
 * Vanilla {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen} renders
 * the actual item stacks on top of these backgrounds.
 */
public class CraftingGridPane implements IDialogPane {

    private static final ResourceLocation CRAFTING_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");

    /** Pane-local Y of the result slot (= CRAFTING_RESULT_Y - CRAFTING_Y). */
    private static final int RESULT_LOCAL_Y = AccessTerminalLayout.CRAFTING_RESULT_Y - AccessTerminalLayout.CRAFTING_Y;

    /** Pane-local Y of the arrow, centred on the crafting rows. */
    private static final int ARROW_LOCAL_Y = (AccessTerminalLayout.CRAFTING_H - AccessTerminalLayout.ARROW_H) / 2;

    @Override
    public int preferredHeight() {
        return AccessTerminalLayout.PLAYER_INV_Y - AccessTerminalLayout.CRAFTING_Y;
    }

    @Override
    public void render(
            GuiGraphics graphics, Font font, int width, int localMouseX, int localMouseY, float partialTick) {
        // 3×3 crafting grid slot backgrounds
        for (int row = 0; row < AccessTerminalLayout.CRAFTING_ROWS; row++) {
            for (int col = 0; col < AccessTerminalLayout.CRAFTING_ROWS; col++) {
                drawSlotBg(
                        graphics,
                        AccessTerminalLayout.CRAFTING_GRID_X + col * AccessTerminalLayout.SLOT_SIZE,
                        row * AccessTerminalLayout.SLOT_SIZE);
            }
        }

        // Arrow from crafting_table.png, centred vertically on the grid
        graphics.blit(
                CRAFTING_TEXTURE,
                AccessTerminalLayout.ARROW_X,
                ARROW_LOCAL_Y,
                AccessTerminalLayout.ARROW_SRC_X,
                AccessTerminalLayout.ARROW_SRC_Y,
                AccessTerminalLayout.ARROW_W,
                AccessTerminalLayout.ARROW_H);

        // Result slot background
        drawSlotBg(graphics, AccessTerminalLayout.CRAFTING_RESULT_X, RESULT_LOCAL_Y);
    }

    private static void drawSlotBg(GuiGraphics graphics, int sx, int sy) {
        graphics.fill(sx, sy, sx + 16, sy + 1, 0xFF373737); // top
        graphics.fill(sx, sy + 1, sx + 1, sy + 16, 0xFF373737); // left
        graphics.fill(sx, sy + 16, sx + 17, sy + 17, 0xFFFFFFFF); // bottom
        graphics.fill(sx + 16, sy, sx + 17, sy + 16, 0xFFFFFFFF); // right
        graphics.fill(sx + 1, sy + 1, sx + 16, sy + 16, 0xFF8B8B8B); // fill
    }
}
