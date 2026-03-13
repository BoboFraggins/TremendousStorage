package net.bobofraggins.intellistore.shared.ui;

import java.util.List;
import javax.annotation.Nullable;
import net.bobofraggins.intellistore.shared.util.CountFormat;
import net.bobofraggins.intellistore.storage.accessterminal.AccessTerminalLayout;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Dialog pane that renders a read-only scrollable item grid backed by a local block inventory.
 *
 * <p>Uses the same visual layout as the network inventory grid in the Access Terminal: 9 columns,
 * 4 visible rows, 18 px slots, and a 6 px scrollbar flush against the right edge of the grid.
 */
public class LocalInventoryPane implements IDialogPane {

    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int GRID_X = AccessTerminalLayout.NETWORK_X;
    private static final int GRID_Y = AccessTerminalLayout.NETWORK_Y - AccessTerminalLayout.TITLE_H; // 1
    private static final int SCROLLBAR_X = AccessTerminalLayout.SCROLLBAR_X;

    // Same height as NetworkInventoryPane: CRAFTING_Y - TITLE_H = 77
    private static final int HEIGHT = AccessTerminalLayout.CRAFTING_Y - AccessTerminalLayout.TITLE_H;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private List<ItemStack> stacks = List.of();
    private List<Long> counts = List.of();
    private int scrollOffset = 0;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void setContents(List<ItemStack> stacks, List<Long> counts) {
        this.stacks = stacks;
        this.counts = counts;
        clampScroll();
    }

    /**
     * Returns the item stack under the given pane-local mouse position, or {@code null} if
     * the cursor is outside the grid or no item is there.
     */
    @Nullable
    public ItemStack getHoveredStack(double localX, double localY) {
        if (!isInGrid(localX, localY)) return null;
        int col = (int) ((localX - GRID_X) / AccessTerminalLayout.SLOT_SIZE);
        int row = (int) ((localY - GRID_Y) / AccessTerminalLayout.SLOT_SIZE);
        int idx = (row + scrollOffset) * AccessTerminalLayout.NETWORK_COLS + col;
        return (idx >= 0 && idx < stacks.size()) ? stacks.get(idx) : null;
    }

    // -------------------------------------------------------------------------
    // IDialogPane
    // -------------------------------------------------------------------------

    @Override
    public int preferredWidth() {
        return AccessTerminalLayout.BG_WIDTH;
    }

    @Override
    public int preferredHeight() {
        return HEIGHT;
    }

    @Override
    public void render(
            GuiGraphics graphics, Font font, int width, int localMouseX, int localMouseY, float partialTick) {
        drawGrid(graphics, font);
        drawScrollbar(graphics);
    }

    @Override
    public boolean mouseScrolled(double localX, double localY, double dx, double dy) {
        if (isInGrid(localX, localY)) {
            scrollOffset -= (int) Math.signum(dy);
            clampScroll();
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean isInGrid(double localX, double localY) {
        return localX >= GRID_X
                && localX < GRID_X + AccessTerminalLayout.NETWORK_W
                && localY >= GRID_Y
                && localY < GRID_Y + AccessTerminalLayout.NETWORK_H;
    }

    private void clampScroll() {
        int totalRows = (stacks.size() + AccessTerminalLayout.NETWORK_COLS - 1) / AccessTerminalLayout.NETWORK_COLS;
        int maxScroll = Math.max(0, totalRows - AccessTerminalLayout.NETWORK_VISIBLE_ROWS);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private void drawGrid(GuiGraphics graphics, Font font) {
        for (int row = 0; row < AccessTerminalLayout.NETWORK_VISIBLE_ROWS; row++) {
            graphics.blit(
                    BG_TEXTURE,
                    GRID_X,
                    GRID_Y + row * AccessTerminalLayout.SLOT_SIZE,
                    7,
                    17 + row * AccessTerminalLayout.SLOT_SIZE,
                    AccessTerminalLayout.NETWORK_W,
                    AccessTerminalLayout.SLOT_SIZE);
        }

        if (stacks.isEmpty()) return;

        int firstIdx = scrollOffset * AccessTerminalLayout.NETWORK_COLS;
        for (int row = 0; row < AccessTerminalLayout.NETWORK_VISIBLE_ROWS; row++) {
            for (int col = 0; col < AccessTerminalLayout.NETWORK_COLS; col++) {
                int idx = firstIdx + row * AccessTerminalLayout.NETWORK_COLS + col;
                if (idx >= stacks.size()) return;

                ItemStack stack = stacks.get(idx);
                long count = counts.get(idx);
                int sx = GRID_X + col * AccessTerminalLayout.SLOT_SIZE + 1;
                int sy = GRID_Y + row * AccessTerminalLayout.SLOT_SIZE + 1;

                graphics.renderItem(stack, sx, sy);
                String countStr = count > 1 ? CountFormat.format(count) : null;
                graphics.renderItemDecorations(font, stack, sx, sy, countStr);
            }
        }
    }

    private void drawScrollbar(GuiGraphics graphics) {
        int barY = GRID_Y;
        int barH = AccessTerminalLayout.NETWORK_H;

        graphics.fill(SCROLLBAR_X, barY, SCROLLBAR_X + AccessTerminalLayout.SCROLLBAR_W, barY + barH, 0x40000000);

        int totalRows = Math.max(
                (stacks.size() + AccessTerminalLayout.NETWORK_COLS - 1) / AccessTerminalLayout.NETWORK_COLS, 1);
        int thumbH = Math.max(8, barH * AccessTerminalLayout.NETWORK_VISIBLE_ROWS / totalRows);
        int maxScroll = Math.max(1, totalRows - AccessTerminalLayout.NETWORK_VISIBLE_ROWS);
        int thumbY = barY + (barH - thumbH) * scrollOffset / maxScroll;
        if (totalRows <= AccessTerminalLayout.NETWORK_VISIBLE_ROWS) {
            thumbY = barY;
            thumbH = barH;
        }
        graphics.fill(SCROLLBAR_X, thumbY, SCROLLBAR_X + AccessTerminalLayout.SCROLLBAR_W, thumbY + thumbH, 0xC0FFFFFF);
    }
}
