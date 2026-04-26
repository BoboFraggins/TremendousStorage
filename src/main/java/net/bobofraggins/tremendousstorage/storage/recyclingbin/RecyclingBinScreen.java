package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import net.bobofraggins.tremendousstorage.shared.ui.Dialog;
import net.bobofraggins.tremendousstorage.shared.ui.PlayerInventoryPane;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Screen for the Recycling Bin.
 *
 * <p>Layout (176 × 175 px via {@link Dialog}):
 * <pre>
 *   ┌─ Dialog title bar (17 px) ─────────────────┐
 *   │  [Void Items]    │  Fill/Drain              │  content (73 px)
 *   │  [void slot]     │  [input slot]            │
 *   │                  │    ↓                     │
 *   │                  │  [output slot]           │
 *   │  Player inventory (3 rows + hotbar, 80 px)  │
 *   └─ Dialog bottom border (5 px) ───────────────┘
 * </pre>
 */
public class RecyclingBinScreen extends AbstractContainerScreen<RecyclingBinMenu> {

    // Height of the content pane above the player inventory.
    // Derived from: INV_Y(90) - Dialog.TITLE_H(17) = 73.
    private static final int CONTENT_PANE_H = 73;

    private final Dialog dialog;

    public RecyclingBinScreen(RecyclingBinMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        dialog = new Dialog(
                Dialog.blankPane(PlayerInventoryPane.WIDTH, CONTENT_PANE_H),
                new PlayerInventoryPane(RecyclingBinMenu.INV_START_X));
        imageWidth = dialog.totalWidth();
        imageHeight = dialog.totalHeight();
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        dialog.render(g, font, title, mouseX, mouseY, partialTick);

        // Vertical divider between void pane (left) and fluid-transfer pane (right)
        g.fill(x + 88, y + Dialog.TITLE_H, x + 89, y + Dialog.TITLE_H + CONTENT_PANE_H, 0xFF555555);

        // Left pane: void slot + label
        drawSlot(g, x + RecyclingBinMenu.VOID_SLOT_X, y + RecyclingBinMenu.VOID_SLOT_Y);
        Component voidLabel = Component.translatable("screen.tremendousstorage.void_items");
        g.drawString(font, voidLabel, x + 44 - font.width(voidLabel) / 2, y + Dialog.TITLE_H + 2, 0x404040, false);

        // Right pane: fluid slots + label + arrow
        drawSlot(g, x + RecyclingBinMenu.FLUID_IN_X, y + RecyclingBinMenu.FLUID_IN_Y);
        drawSlot(g, x + RecyclingBinMenu.FLUID_OUT_X, y + RecyclingBinMenu.FLUID_OUT_Y);
        drawDownArrow(g, x + RecyclingBinMenu.FLUID_IN_X, y + RecyclingBinMenu.FLUID_IN_Y + 16);
        Component fillLabel = Component.translatable("screen.tremendousstorage.fluid_transfer");
        g.drawString(font, fillLabel, x + 132 - font.width(fillLabel) / 2, y + Dialog.TITLE_H + 2, 0x404040, false);

        // Ghost hint in fill slot when empty
        drawGhostFillHint(g, x + RecyclingBinMenu.FLUID_IN_X, y + RecyclingBinMenu.FLUID_IN_Y, partialTick);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Title is drawn by Dialog; no "Inventory" label.
    }

    // -------------------------------------------------------------------------
    // Drawing helpers
    // -------------------------------------------------------------------------

    private static void drawSlot(GuiGraphics g, int sx, int sy) {
        g.fill(sx, sy, sx + 16, sy + 1, 0xFF373737); // top
        g.fill(sx, sy + 1, sx + 1, sy + 16, 0xFF373737); // left
        g.fill(sx, sy + 16, sx + 17, sy + 17, 0xFFFFFFFF); // bottom
        g.fill(sx + 16, sy, sx + 17, sy + 16, 0xFFFFFFFF); // right
        g.fill(sx + 1, sy + 1, sx + 16, sy + 16, 0xFF8B8B8B); // interior
    }

    private static void drawDownArrow(GuiGraphics g, int gapX, int gapY) {
        int cx = gapX + 8;
        int top = gapY + 3;
        g.fill(cx - 1, top, cx + 1, top + 9, 0xFF555555); // stem
        g.fill(cx - 4, top + 9, cx + 4, top + 11, 0xFF555555); // 8 px wide
        g.fill(cx - 2, top + 11, cx + 2, top + 13, 0xFF555555); // 4 px wide
        g.fill(cx - 1, top + 13, cx + 1, top + 15, 0xFF555555); // 2 px wide
    }

    private void drawGhostFillHint(GuiGraphics g, int sx, int sy, float partialTick) {
        if (!menu.getSlot(1).getItem().isEmpty()) return;
        if (minecraft == null || minecraft.player == null) return;

        // 80-tick cycle: first 40 ticks show bucket, next 40 show glass bottle.
        long tick = minecraft.player.tickCount;
        int cyclePos = (int) (tick % 80);
        ItemStack ghost = cyclePos < 40 ? new ItemStack(Items.BUCKET) : new ItemStack(Items.GLASS_BOTTLE);

        // Sine-wave overlay: alpha ranges from 0x50 (visible) to 0xC0 (dim).
        double phase = ((cyclePos % 40) + partialTick) * Math.PI / 20.0;
        int overlayAlpha = 0x50 + (int) (0x70 * (0.5 + 0.5 * Math.sin(phase)));

        g.renderItem(ghost, sx, sy);
        g.fill(sx, sy, sx + 16, sy + 16, overlayAlpha << 24);
    }
}
