package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Recycling Bin.
 *
 * <p>Layout (176 × 168 px):
 * <pre>
 *   ┌─ 0 ─────────────────────────── 176 ─┐
 *   │  Title                            14│  title bar
 *   ├──────────────────────────────────────┤
 *   │  [Void]    │  Fill/Drain            │
 *   │            │  [Input]               │  content (y=14–82)
 *   │            │    ↓                   │
 *   │            │  [Output]              │
 *   ├──────────────────────────────────────┤  y=82
 *   │  Player inventory (3 rows)          │
 *   │  Hotbar                             │
 *   └──────────────────────────────────────┘  168
 * </pre>
 */
public class RecyclingBinScreen extends AbstractContainerScreen<RecyclingBinMenu> {

    private static final int IMG_WIDTH  = 176;
    private static final int IMG_HEIGHT = 168;

    public RecyclingBinScreen(RecyclingBinMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth  = IMG_WIDTH;
        imageHeight = IMG_HEIGHT;
        inventoryLabelY = IMG_HEIGHT - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // Full background
        g.fill(x, y, x + IMG_WIDTH, y + IMG_HEIGHT, 0xFFC6C6C6);

        // Title-bar separator
        g.fill(x, y + 14, x + IMG_WIDTH, y + 15, 0xFF555555);

        // Content / player-inventory separator
        g.fill(x, y + 82, x + IMG_WIDTH, y + 83, 0xFF555555);

        // Vertical divider between void pane and fluid-transfer pane
        g.fill(x + 88, y + 14, x + 89, y + 82, 0xFF555555);

        // Left pane — void slot indicator
        drawSlot(g, x + RecyclingBinMenu.VOID_SLOT_X, y + RecyclingBinMenu.VOID_SLOT_Y);

        // "Void Items" label above the void slot
        Component voidLabel = Component.translatable("screen.tremendousstorage.void_items");
        g.drawString(font, voidLabel,
                x + 44 - font.width(voidLabel) / 2, y + 17, 0x404040, false);

        // Right pane — fluid-transfer slots and arrow
        drawSlot(g, x + RecyclingBinMenu.FLUID_IN_X,  y + RecyclingBinMenu.FLUID_IN_Y);
        drawSlot(g, x + RecyclingBinMenu.FLUID_OUT_X, y + RecyclingBinMenu.FLUID_OUT_Y);
        drawDownArrow(g, x + RecyclingBinMenu.FLUID_IN_X, y + RecyclingBinMenu.FLUID_IN_Y + 16);

        // "Fill / Drain" label above the input slot
        Component fillLabel = Component.translatable("screen.tremendousstorage.fluid_transfer");
        g.drawString(font, fillLabel,
                x + 132 - font.width(fillLabel) / 2, y + 17, 0x404040, false);

        // Player-inventory slot backgrounds
        drawSlotGrid(g, x, y, 8, 90,  9, 3);
        drawSlotGrid(g, x, y, 8, 148, 9, 1);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (IMG_WIDTH - font.width(title)) / 2, 4, 0x404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    // -------------------------------------------------------------------------
    // Drawing helpers
    // -------------------------------------------------------------------------

    private static void drawSlot(GuiGraphics g, int sx, int sy) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
        g.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
    }

    private static void drawSlotGrid(GuiGraphics g, int ox, int oy, int startX, int startY, int cols, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                drawSlot(g, ox + startX + col * 18, oy + startY + row * 18);
            }
        }
    }

    private static void drawDownArrow(GuiGraphics g, int gapX, int gapY) {
        int cx  = gapX + 8;
        int top = gapY + 3;
        g.fill(cx - 1, top,      cx + 1, top + 9,  0xFF555555); // stem
        g.fill(cx - 4, top + 9,  cx + 4, top + 11, 0xFF555555); // 8 px
        g.fill(cx - 2, top + 11, cx + 2, top + 13, 0xFF555555); // 4 px
        g.fill(cx - 1, top + 13, cx + 1, top + 15, 0xFF555555); // 2 px
    }
}
