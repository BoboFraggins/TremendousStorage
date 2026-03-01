package net.bobofraggins.intellistore.storage.accessterminal;

import java.util.List;
import net.bobofraggins.intellistore.shared.network.RequestSatContentsPacket;
import net.bobofraggins.intellistore.shared.network.SatContentsPacket;
import net.bobofraggins.intellistore.shared.network.SatExtractPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen for the Storage Access Terminal.
 *
 * <p>Layout (image-relative coordinates):
 * <ul>
 *   <li>y=0..17   title bar (from vanilla generic_54 texture)
 *   <li>y=17..129 scrollable network item list (7 rows × 16px)
 *   <li>y=133..187 3×3 crafting grid + result slot  (4px gap above and below)
 *   <li>y=191..257 player inventory (3 rows × 18px)
 *   <li>y=261..279 player hotbar (1 row × 18px)
 *   <li>full height: 283
 * </ul>
 */
public class AccessTerminalScreen extends AbstractContainerScreen<AccessTerminalMenu> {

    // -------------------------------------------------------------------------
    // Texture locations
    // -------------------------------------------------------------------------

    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    // Vanilla crafting table texture — used for the arrow sprite
    private static final ResourceLocation CRAFTING_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");

    // -------------------------------------------------------------------------
    // Layout constants  (all image-relative, i.e. relative to leftPos/topPos)
    // -------------------------------------------------------------------------

    private static final int BG_WIDTH   = 176;

    private static final int LIST_Y      = 17;   // immediately below title bar
    private static final int ROW_HEIGHT  = 16;
    private static final int VISIBLE_ROWS = 7;
    private static final int LIST_HEIGHT = VISIBLE_ROWS * ROW_HEIGHT; // 112

    // Scrollbar: 6px wide, right-flush inside the panel border
    private static final int SCROLLBAR_W = 6;
    private static final int SCROLLBAR_X = BG_WIDTH - 1 - SCROLLBAR_W; // 169

    // List content area (left of scrollbar)
    private static final int LIST_CONTENT_X = 1;  // 1px inside left border
    private static final int LIST_CONTENT_W = SCROLLBAR_X - LIST_CONTENT_X - 1; // leaves 1px gap before bar

    // Crafting section: 4px gap below the list
    private static final int CRAFT_GAP  = 4;
    private static final int CRAFT_Y    = LIST_Y + LIST_HEIGHT + CRAFT_GAP; // 133
    private static final int CRAFT_ROWS = 3;

    // Crafting grid is centred horizontally: 3 slots × 18 = 54px.
    // Arrow is 22px wide, 2px gap on each side. Result slot is 18px wide.
    // Total: 54 + 2 + 22 + 2 + 18 = 98px. Centred in 176: (176-98)/2 = 39px left margin.
    private static final int GRID_X     = 30;   // left edge of the 3×3 grid
    private static final int ARROW_X    = GRID_X + 3 * 18 + 2; // 86
    private static final int RESULT_X   = ARROW_X + 22 + 2;    // 110

    // Result slot is 18px wide; centre vertically in craft area (3 rows × 18 = 54px)
    private static final int RESULT_Y   = CRAFT_Y + 18; // vertically centred: row 1 of 3

    // Player inventory: same gap below crafting as crafting has above player inv
    private static final int INV_Y      = CRAFT_Y + CRAFT_ROWS * 18 + CRAFT_GAP; // 191
    private static final int HOTBAR_Y   = INV_Y + 3 * 18 + CRAFT_GAP;            // 249

    private static final int BG_HEIGHT  = HOTBAR_Y + 18 + CRAFT_GAP; // 271

    // Crafting arrow source coords in crafting_table.png (256×256)
    private static final int ARROW_SRC_X = 82;
    private static final int ARROW_SRC_Y = 60;
    private static final int ARROW_W     = 22;
    private static final int ARROW_H     = 15;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private List<ItemStack> networkStacks = List.of();
    private List<Long>      networkCounts = List.of();
    private int scrollOffset = 0;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public AccessTerminalScreen(AccessTerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    @Override
    protected void init() {
        super.init();
        if (menu.hasNetwork()) {
            PacketDistributor.sendToServer(new RequestSatContentsPacket(menu.getNiPos()));
        }
        SatContentsPacket.PENDING_STACKS = List.of();
        SatContentsPacket.PENDING_COUNTS = List.of();
        networkStacks = List.of();
        networkCounts = List.of();
        scrollOffset = 0;
    }

    // -------------------------------------------------------------------------
    // Tick
    // -------------------------------------------------------------------------

    @Override
    protected void containerTick() {
        super.containerTick();
        List<ItemStack> pending = SatContentsPacket.PENDING_STACKS;
        if (!pending.isEmpty() && pending != networkStacks) {
            networkStacks = pending;
            networkCounts = SatContentsPacket.PENDING_COUNTS;
            clampScroll();
        }
    }

    // -------------------------------------------------------------------------
    // Scroll
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInListArea(mouseX, mouseY)) {
            scrollOffset -= (int) Math.signum(scrollY);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void clampScroll() {
        int maxScroll = Math.max(0, networkStacks.size() - VISIBLE_ROWS);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private boolean isInListArea(double mx, double my) {
        return mx >= leftPos + LIST_CONTENT_X
                && mx < leftPos + SCROLLBAR_X
                && my >= topPos + LIST_Y
                && my < topPos + LIST_Y + LIST_HEIGHT;
    }

    // -------------------------------------------------------------------------
    // Mouse click
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInListArea(mouseX, mouseY)) {
            int relY    = (int) (mouseY - topPos - LIST_Y);
            int rowIndex = relY / ROW_HEIGHT + scrollOffset;
            if (rowIndex >= 0 && rowIndex < networkStacks.size() && menu.hasNetwork()) {
                ItemStack target    = networkStacks.get(rowIndex);
                long      totalCount = networkCounts.get(rowIndex);
                int amount = (int) Math.min(totalCount, target.getMaxStackSize());
                PacketDistributor.sendToServer(new SatExtractPacket(menu.getNiPos(), target.copyWithCount(1), amount));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        if (isInListArea(mouseX, mouseY)) {
            int relY     = (int) (mouseY - topPos - LIST_Y);
            int rowIndex = relY / ROW_HEIGHT + scrollOffset;
            if (rowIndex >= 0 && rowIndex < networkStacks.size()) {
                graphics.renderTooltip(font, networkStacks.get(rowIndex), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;

        // ── Title bar: top 17px of generic_54 texture ──
        graphics.blit(BG_TEXTURE, x, y, 0, 0, BG_WIDTH, 17);

        // ── Middle fill (list + craft area) ──
        // The vanilla player-inv section from the texture starts at src y=126.
        // It is 96px tall, containing 7px of header padding before the first slot row.
        // We want slot outlines to land at topPos + INV_Y, so we blit at topPos + INV_Y - 7.
        int invBgDest = y + INV_Y - 7;

        // Fill the entire middle+inv area with the panel background color
        graphics.fill(x, y + 17, x + BG_WIDTH, y + BG_HEIGHT, 0xFFC6C6C6);

        // Left and right border lines spanning the entire panel (below title bar)
        graphics.fill(x,               y + 17, x + 1,           y + BG_HEIGHT, 0xFF555555); // left dark
        graphics.fill(x + BG_WIDTH - 1, y + 17, x + BG_WIDTH,    y + BG_HEIGHT, 0xFFFFFFFF); // right bright

        // Bottom border
        graphics.fill(x, y + BG_HEIGHT - 1, x + BG_WIDTH, y + BG_HEIGHT, 0xFF555555);

        // ── Player inventory section from vanilla texture ──
        // Blit the slot outlines only (strip the grey background since we filled it above).
        // generic_54.png: slots are at src x=7, y=140 for the first 3×9 rows (3×9×18 = 486px range).
        // We just blit the full 96px section; it blends seamlessly over our grey fill.
        graphics.blit(BG_TEXTURE, x, invBgDest, 0, 126, BG_WIDTH, 96);

        // ── Network item list ──
        drawNetworkList(graphics, x, y);

        // ── Scrollbar (always shown) ──
        drawScrollbar(graphics, x, y);

        // ── Crafting section ──
        drawCraftingSection(graphics, x, y);
    }

    private void drawNetworkList(GuiGraphics graphics, int x, int y) {
        int listX0 = x + LIST_CONTENT_X;
        int listX1 = x + SCROLLBAR_X - 1;
        int listY0 = y + LIST_Y;
        int listY1 = listY0 + LIST_HEIGHT;

        // Draw empty slot backgrounds for all VISIBLE_ROWS rows
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int rowY = listY0 + i * ROW_HEIGHT;
            // Slot-style inset: 1px dark top+left, 1px bright bottom+right, gray interior
            drawSlotBackground(graphics, listX0, rowY, LIST_CONTENT_W, ROW_HEIGHT);
        }

        // Scissor to list content area
        graphics.enableScissor(listX0, listY0, listX1, listY1);

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx  = i + scrollOffset;
            if (idx >= networkStacks.size()) break;

            ItemStack stack = networkStacks.get(idx);
            long      count = networkCounts.get(idx);
            int rowX = listX0 + 1;
            int rowY = listY0 + i * ROW_HEIGHT;

            // Item icon (16×16, centred vertically in the row)
            graphics.renderItem(stack, rowX, rowY);
            graphics.renderItemDecorations(font, stack, rowX, rowY, null);

            // Item name
            String name     = stack.getDisplayName().getString();
            graphics.drawString(font, name, rowX + 18, rowY + 4, 0xE0E0E0, false);

            // Count
            String countStr = abbreviateCount(count);
            int    countX   = x + SCROLLBAR_X - 2 - font.width(countStr);
            graphics.drawString(font, countStr, countX, rowY + 4, 0xAAAAAA, false);
        }

        graphics.disableScissor();
    }

    private void drawScrollbar(GuiGraphics graphics, int x, int y) {
        int barX  = x + SCROLLBAR_X;
        int barY  = y + LIST_Y;
        int barH  = LIST_HEIGHT;

        // Track
        graphics.fill(barX, barY, barX + SCROLLBAR_W, barY + barH, 0x40000000);

        // Thumb
        int total   = Math.max(networkStacks.size(), 1);
        int thumbH  = Math.max(8, barH * VISIBLE_ROWS / total);
        int maxScroll = Math.max(1, total - VISIBLE_ROWS);
        int thumbY  = barY + (barH - thumbH) * scrollOffset / maxScroll;
        if (networkStacks.size() <= VISIBLE_ROWS) {
            // All content fits — thumb fills the whole track
            thumbY = barY;
            thumbH = barH;
        }
        graphics.fill(barX, thumbY, barX + SCROLLBAR_W, thumbY + thumbH, 0xC0FFFFFF);
    }

    private void drawCraftingSection(GuiGraphics graphics, int x, int y) {
        // 3×3 crafting grid slots (vanilla inset style)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = x + GRID_X + col * 18;
                int sy = y + CRAFT_Y + row * 18;
                drawSlotBackground(graphics, sx, sy, 16, 16);
            }
        }

        // Arrow from vanilla crafting_table.png
        // Arrow is vertically centred in the craft area (54px tall): top of craft area + 18 + (18-15)/2
        int arrowDestX = x + ARROW_X;
        int arrowDestY = y + CRAFT_Y + 18 + (18 - ARROW_H) / 2; // vertically centred in middle row
        graphics.blit(CRAFTING_TEXTURE, arrowDestX, arrowDestY, ARROW_SRC_X, ARROW_SRC_Y, ARROW_W, ARROW_H);

        // Result slot (18×18 inset)
        drawSlotBackground(graphics, x + RESULT_X, y + RESULT_Y, 16, 16);
    }

    /**
     * Draws a vanilla-style inset slot: 1px dark shadow on top+left edges,
     * 1px bright highlight on bottom+right edges, gray interior fill.
     */
    private static void drawSlotBackground(GuiGraphics graphics, int sx, int sy, int w, int h) {
        // Dark edges (top, left)
        graphics.fill(sx,     sy,     sx + w, sy + 1, 0xFF373737); // top
        graphics.fill(sx,     sy + 1, sx + 1, sy + h, 0xFF373737); // left
        // Bright edges (bottom, right)
        graphics.fill(sx,     sy + h, sx + w + 1, sy + h + 1, 0xFFFFFFFF); // bottom
        graphics.fill(sx + w, sy,     sx + w + 1, sy + h,     0xFFFFFFFF); // right
        // Interior
        graphics.fill(sx + 1, sy + 1, sx + w, sy + h, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title centred, black, shifted down to y=4
        graphics.drawString(font, title, (BG_WIDTH - font.width(title)) / 2, 4, 0x404040, false);
        // No network-status text, no "network is empty" text
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String abbreviateCount(long count) {
        if (count >= 1_000_000) return String.format("%.1fM", count / 1_000_000.0);
        if (count >= 1_000)     return String.format("%.1fk", count / 1_000.0);
        return String.valueOf(count);
    }
}
