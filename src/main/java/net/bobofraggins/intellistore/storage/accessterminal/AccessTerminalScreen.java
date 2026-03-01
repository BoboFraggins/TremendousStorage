package net.bobofraggins.intellistore.storage.accessterminal;

import java.util.List;
import net.bobofraggins.intellistore.shared.network.RequestSatContentsPacket;
import net.bobofraggins.intellistore.shared.network.SatContentsPacket;
import net.bobofraggins.intellistore.shared.network.SatExtractPacket;
import net.bobofraggins.intellistore.shared.network.SatInsertPacket;
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
 *   <li>y=0..17    title bar (from vanilla generic_54 texture)
 *   <li>y=18..90   network grid (4 rows × 9 columns of 18×18 slots, + 6px scrollbar at right)
 *   <li>y=94..148  3×3 crafting grid + result slot  (4px gap above and below)
 *   <li>y=148..202 player inventory (3 rows × 18px)
 *   <li>y=206..224 player hotbar (1 row × 18px)
 *   <li>full height: 228
 * </ul>
 */
public class AccessTerminalScreen extends AbstractContainerScreen<AccessTerminalMenu> {

    // -------------------------------------------------------------------------
    // Texture locations
    // -------------------------------------------------------------------------

    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final ResourceLocation CRAFTING_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");

    // -------------------------------------------------------------------------
    // Layout constants  (all image-relative, i.e. relative to leftPos/topPos)
    // -------------------------------------------------------------------------

    private static final int BG_WIDTH = 176;

    // Network grid — chest-style 9×4 slot area
    private static final int COLS = 9;
    private static final int VISIBLE_ROWS = 4;
    private static final int SLOT_SIZE = 18;
    private static final int GRID_X = 8; // vanilla left margin
    private static final int GRID_Y = 18; // 1px below title bar
    private static final int GRID_W = COLS * SLOT_SIZE; // 162
    private static final int GRID_H = VISIBLE_ROWS * SLOT_SIZE; // 72

    // Scrollbar: flush against right border
    private static final int SCROLLBAR_W = 6;
    private static final int SCROLLBAR_X = BG_WIDTH - 1 - SCROLLBAR_W; // 169

    // Crafting section: 4px gap below the grid
    private static final int CRAFT_GAP = 4;
    private static final int CRAFT_Y = GRID_Y + GRID_H + CRAFT_GAP; // 94
    private static final int CRAFT_ROWS = 3;

    // Crafting grid centred: 3×18=54 + 2 + 22 + 2 + 18 = 98px, (176-98)/2 = 39 left margin
    private static final int CRAFT_GRID_X = 30;
    private static final int ARROW_X = CRAFT_GRID_X + 3 * SLOT_SIZE + 2; // 86
    private static final int RESULT_X = ARROW_X + 22 + 2; // 110
    private static final int RESULT_Y = CRAFT_Y + SLOT_SIZE; // vertically centred (row 1 of 3)

    // Player inventory: gap below crafting
    private static final int INV_Y = CRAFT_Y + CRAFT_ROWS * SLOT_SIZE + CRAFT_GAP; // 148
    private static final int HOTBAR_Y = INV_Y + 3 * SLOT_SIZE + CRAFT_GAP; // 206

    private static final int BG_HEIGHT = HOTBAR_Y + SLOT_SIZE + CRAFT_GAP; // 228

    // Crafting arrow sprite in crafting_table.png
    private static final int ARROW_SRC_X = 82;
    private static final int ARROW_SRC_Y = 60;
    private static final int ARROW_W = 22;
    private static final int ARROW_H = 15;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private List<ItemStack> networkStacks = List.of();
    private List<Long> networkCounts = List.of();
    private int scrollOffset = 0; // in rows

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public AccessTerminalScreen(AccessTerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = BG_WIDTH;
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
        if (isInGridArea(mouseX, mouseY)) {
            scrollOffset -= (int) Math.signum(scrollY);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void clampScroll() {
        int totalRows = (networkStacks.size() + COLS - 1) / COLS;
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private boolean isInGridArea(double mx, double my) {
        return mx >= leftPos + GRID_X
                && mx < leftPos + GRID_X + GRID_W
                && my >= topPos + GRID_Y
                && my < topPos + GRID_Y + GRID_H;
    }

    // -------------------------------------------------------------------------
    // Mouse interaction
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInGridArea(mouseX, mouseY)) {
            int col = (int) ((mouseX - leftPos - GRID_X) / SLOT_SIZE);
            int row = (int) ((mouseY - topPos - GRID_Y) / SLOT_SIZE);
            int idx = (row + scrollOffset) * COLS + col;

            if (idx >= 0 && idx < networkStacks.size() && menu.hasNetwork()) {
                ItemStack target = networkStacks.get(idx);
                long totalCount = networkCounts.get(idx);

                // Right-click: extract single item; left-click: extract full stack
                int amount = (button == 1) ? 1 : (int) Math.min(totalCount, target.getMaxStackSize());
                PacketDistributor.sendToServer(new SatExtractPacket(menu.getNiPos(), target.copyWithCount(1), amount));
                return true;
            }
            return true; // consume click even on empty cell
        }

        // Left-click with a held item on the grid area: handled above.
        // If the player releases a drag or clicks elsewhere, fall through.
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * When the player releases the mouse over the network grid while holding an item
     * on the cursor, insert the held stack into the network.
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isInGridArea(mouseX, mouseY) && menu.hasNetwork()) {
            ItemStack carried = menu.getCarried();
            if (!carried.isEmpty()) {
                // Find the player inventory slot that contains this stack.
                // The carried stack is in the cursor — we need to send it to the network.
                // Use slot index -1 as a sentinel: the server will take it from the cursor.
                PacketDistributor.sendToServer(new SatInsertPacket(menu.getNiPos(), -1));
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        // Tooltip for hovered network slot
        if (isInGridArea(mouseX, mouseY)) {
            int col = (int) ((mouseX - leftPos - GRID_X) / SLOT_SIZE);
            int row = (int) ((mouseY - topPos - GRID_Y) / SLOT_SIZE);
            int idx = (row + scrollOffset) * COLS + col;
            if (idx >= 0 && idx < networkStacks.size()) {
                graphics.renderTooltip(font, networkStacks.get(idx), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;

        // Title bar (top 17px of generic_54)
        graphics.blit(BG_TEXTURE, x, y, 0, 0, BG_WIDTH, 17);

        // Gray fill for entire panel body
        graphics.fill(x, y + 17, x + BG_WIDTH, y + BG_HEIGHT, 0xFFC6C6C6);

        // Left and right border lines
        graphics.fill(x, y + 17, x + 1, y + BG_HEIGHT, 0xFF555555);
        graphics.fill(x + BG_WIDTH - 1, y + 17, x + BG_WIDTH, y + BG_HEIGHT, 0xFFFFFFFF);

        // Bottom border
        graphics.fill(x, y + BG_HEIGHT - 1, x + BG_WIDTH, y + BG_HEIGHT, 0xFF555555);

        // Player inventory slot outlines from vanilla texture.
        // Src y=126 has 7px padding then slot rows; blit at INV_Y-7 so outlines land at INV_Y.
        graphics.blit(BG_TEXTURE, x, y + INV_Y - 7, 0, 126, BG_WIDTH, 96);

        // Network grid
        drawNetworkGrid(graphics, x, y);

        // Scrollbar
        drawScrollbar(graphics, x, y);

        // Crafting section
        drawCraftingSection(graphics, x, y);
    }

    private void drawNetworkGrid(GuiGraphics graphics, int x, int y) {
        // Blit slot outlines from generic_54.png chest rows (src x=0, y=17, each row 18px tall)
        // Blit 4 rows of 9 slots each, at GRID_X=8, GRID_Y=18
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            // Each chest row in the texture: src y = 17 + row * 18, height 18, width = 9*18 = 162
            graphics.blit(
                    BG_TEXTURE, x + GRID_X, y + GRID_Y + row * SLOT_SIZE, 7, 17 + row * SLOT_SIZE, GRID_W, SLOT_SIZE);
        }

        // Overlay item icons and counts
        int firstIdx = scrollOffset * COLS;
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int idx = firstIdx + row * COLS + col;
                if (idx >= networkStacks.size()) break;

                ItemStack stack = networkStacks.get(idx);
                long count = networkCounts.get(idx);
                int sx = x + GRID_X + col * SLOT_SIZE + 1;
                int sy = y + GRID_Y + row * SLOT_SIZE + 1;

                graphics.renderItem(stack, sx, sy);

                // Show count only if > 1
                if (count > 1) {
                    String countStr = abbreviateCount(count);
                    graphics.renderItemDecorations(font, stack, sx, sy, countStr);
                } else {
                    graphics.renderItemDecorations(font, stack, sx, sy, null);
                }
            }
        }
    }

    private void drawScrollbar(GuiGraphics graphics, int x, int y) {
        int barX = x + SCROLLBAR_X;
        int barY = y + GRID_Y;
        int barH = GRID_H;

        // Track
        graphics.fill(barX, barY, barX + SCROLLBAR_W, barY + barH, 0x40000000);

        // Thumb
        int totalRows = Math.max((networkStacks.size() + COLS - 1) / COLS, 1);
        int thumbH = Math.max(8, barH * VISIBLE_ROWS / totalRows);
        int maxScroll = Math.max(1, totalRows - VISIBLE_ROWS);
        int thumbY = barY + (barH - thumbH) * scrollOffset / maxScroll;
        if (totalRows <= VISIBLE_ROWS) {
            thumbY = barY;
            thumbH = barH;
        }
        graphics.fill(barX, thumbY, barX + SCROLLBAR_W, thumbY + thumbH, 0xC0FFFFFF);
    }

    private void drawCraftingSection(GuiGraphics graphics, int x, int y) {
        // 3×3 crafting grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlotBackground(graphics, x + CRAFT_GRID_X + col * SLOT_SIZE, y + CRAFT_Y + row * SLOT_SIZE, 16, 16);
            }
        }

        // Arrow
        int arrowDestY = y + CRAFT_Y + SLOT_SIZE + (SLOT_SIZE - ARROW_H) / 2;
        graphics.blit(CRAFTING_TEXTURE, x + ARROW_X, arrowDestY, ARROW_SRC_X, ARROW_SRC_Y, ARROW_W, ARROW_H);

        // Result slot
        drawSlotBackground(graphics, x + RESULT_X, y + RESULT_Y, 16, 16);
    }

    private static void drawSlotBackground(GuiGraphics graphics, int sx, int sy, int w, int h) {
        graphics.fill(sx, sy, sx + w, sy + 1, 0xFF373737); // top
        graphics.fill(sx, sy + 1, sx + 1, sy + h, 0xFF373737); // left
        graphics.fill(sx, sy + h, sx + w + 1, sy + h + 1, 0xFFFFFFFF); // bottom
        graphics.fill(sx + w, sy, sx + w + 1, sy + h, 0xFFFFFFFF); // right
        graphics.fill(sx + 1, sy + 1, sx + w, sy + h, 0xFF8B8B8B); // interior
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (BG_WIDTH - font.width(title)) / 2, 4, 0x404040, false);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String abbreviateCount(long count) {
        if (count >= 1_000_000) return String.format("%.1fM", count / 1_000_000.0);
        if (count >= 1_000) return String.format("%.1fk", count / 1_000.0);
        return String.valueOf(count);
    }
}
