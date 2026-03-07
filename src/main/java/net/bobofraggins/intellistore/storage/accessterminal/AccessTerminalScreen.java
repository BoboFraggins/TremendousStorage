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
 * <p>All pixel coordinates come from {@link AccessTerminalLayout} — the single source of truth
 * shared with {@link AccessTerminalMenu}.
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
    // State
    // -------------------------------------------------------------------------

    private List<ItemStack> networkStacks = null; // null = awaiting first server response
    private List<Long> networkCounts = List.of();
    private int scrollOffset = 0; // in rows

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public AccessTerminalScreen(AccessTerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AccessTerminalLayout.BG_WIDTH;
        this.imageHeight = AccessTerminalLayout.BG_HEIGHT;
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
        SatContentsPacket.PENDING_STACKS = null;
        SatContentsPacket.PENDING_COUNTS = List.of();
        networkStacks = null;
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
        if (pending != null && pending != networkStacks) {
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
        if (networkStacks == null) return;
        int totalRows =
                (networkStacks.size() + AccessTerminalLayout.NETWORK_COLS - 1) / AccessTerminalLayout.NETWORK_COLS;
        int maxScroll = Math.max(0, totalRows - AccessTerminalLayout.NETWORK_VISIBLE_ROWS);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private boolean isInGridArea(double mx, double my) {
        return mx >= leftPos + AccessTerminalLayout.NETWORK_X
                && mx < leftPos + AccessTerminalLayout.NETWORK_X + AccessTerminalLayout.NETWORK_W
                && my >= topPos + AccessTerminalLayout.NETWORK_Y
                && my < topPos + AccessTerminalLayout.NETWORK_Y + AccessTerminalLayout.NETWORK_H;
    }

    // -------------------------------------------------------------------------
    // Mouse interaction
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInGridArea(mouseX, mouseY)) {
            ItemStack carried = menu.getCarried();

            if (!carried.isEmpty()) {
                // Cursor has items — deposit into network on left-click
                if (button == 0 && menu.hasNetwork()) {
                    PacketDistributor.sendToServer(new SatInsertPacket(menu.getNiPos(), -1));
                }
                return true;
            }

            // Cursor empty — extract from network to cursor
            int col = (int) ((mouseX - leftPos - AccessTerminalLayout.NETWORK_X) / AccessTerminalLayout.SLOT_SIZE);
            int row = (int) ((mouseY - topPos - AccessTerminalLayout.NETWORK_Y) / AccessTerminalLayout.SLOT_SIZE);
            int idx = (row + scrollOffset) * AccessTerminalLayout.NETWORK_COLS + col;

            if (networkStacks != null && idx >= 0 && idx < networkStacks.size() && menu.hasNetwork()) {
                ItemStack target = networkStacks.get(idx);
                long totalCount = networkCounts.get(idx);
                int maxStack = target.getMaxStackSize();
                if (hasShiftDown()) {
                    // Shift-click: send full stack directly to inventory
                    int amount = (int) Math.min(totalCount, maxStack);
                    PacketDistributor.sendToServer(
                            new SatExtractPacket(menu.getNiPos(), target.copyWithCount(1), amount, false));
                } else {
                    // Right-click: 1 to cursor; left-click: full stack to cursor
                    int amount = (button == 1) ? 1 : (int) Math.min(totalCount, maxStack);
                    PacketDistributor.sendToServer(
                            new SatExtractPacket(menu.getNiPos(), target.copyWithCount(1), amount, true));
                }
            }
            return true; // consume click even on empty cell
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

        // Tooltip for hovered network slot
        if (isInGridArea(mouseX, mouseY)) {
            int col = (int) ((mouseX - leftPos - AccessTerminalLayout.NETWORK_X) / AccessTerminalLayout.SLOT_SIZE);
            int row = (int) ((mouseY - topPos - AccessTerminalLayout.NETWORK_Y) / AccessTerminalLayout.SLOT_SIZE);
            int idx = (row + scrollOffset) * AccessTerminalLayout.NETWORK_COLS + col;
            if (networkStacks != null && idx >= 0 && idx < networkStacks.size()) {
                graphics.renderTooltip(font, networkStacks.get(idx), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;

        // Title bar (top TITLE_H px of generic_54)
        graphics.blit(BG_TEXTURE, x, y, 0, 0, AccessTerminalLayout.BG_WIDTH, AccessTerminalLayout.TITLE_H);

        // Gray fill for entire panel body
        graphics.fill(
                x,
                y + AccessTerminalLayout.TITLE_H,
                x + AccessTerminalLayout.BG_WIDTH,
                y + AccessTerminalLayout.BG_HEIGHT,
                0xFFC6C6C6);

        // Left and right border lines
        graphics.fill(x, y + AccessTerminalLayout.TITLE_H, x + 1, y + AccessTerminalLayout.BG_HEIGHT, 0xFF555555);
        graphics.fill(
                x + AccessTerminalLayout.BG_WIDTH - 1,
                y + AccessTerminalLayout.TITLE_H,
                x + AccessTerminalLayout.BG_WIDTH,
                y + AccessTerminalLayout.BG_HEIGHT,
                0xFFFFFFFF);

        // Bottom border
        graphics.fill(
                x,
                y + AccessTerminalLayout.BG_HEIGHT - 1,
                x + AccessTerminalLayout.BG_WIDTH,
                y + AccessTerminalLayout.BG_HEIGHT,
                0xFF555555);

        // Player inventory slot outlines from vanilla texture.
        // Src y=126 has 7px padding then slot rows; blit at PLAYER_INV_Y-7 so outlines land at PLAYER_INV_Y.
        graphics.blit(
                BG_TEXTURE, x, y + AccessTerminalLayout.PLAYER_INV_Y - 7, 0, 126, AccessTerminalLayout.BG_WIDTH, 96);

        // Network grid
        drawNetworkGrid(graphics, x, y);

        // Scrollbar
        drawScrollbar(graphics, x, y);

        // Crafting section
        drawCraftingSection(graphics, x, y);
    }

    private void drawNetworkGrid(GuiGraphics graphics, int x, int y) {
        // Blit slot outlines from generic_54.png chest rows
        for (int row = 0; row < AccessTerminalLayout.NETWORK_VISIBLE_ROWS; row++) {
            graphics.blit(
                    BG_TEXTURE,
                    x + AccessTerminalLayout.NETWORK_X,
                    y + AccessTerminalLayout.NETWORK_Y + row * AccessTerminalLayout.SLOT_SIZE,
                    7,
                    17 + row * AccessTerminalLayout.SLOT_SIZE,
                    AccessTerminalLayout.NETWORK_W,
                    AccessTerminalLayout.SLOT_SIZE);
        }

        // Overlay item icons and counts
        if (networkStacks == null) return;
        int firstIdx = scrollOffset * AccessTerminalLayout.NETWORK_COLS;
        for (int row = 0; row < AccessTerminalLayout.NETWORK_VISIBLE_ROWS; row++) {
            for (int col = 0; col < AccessTerminalLayout.NETWORK_COLS; col++) {
                int idx = firstIdx + row * AccessTerminalLayout.NETWORK_COLS + col;
                if (idx >= networkStacks.size()) break;

                ItemStack stack = networkStacks.get(idx);
                long count = networkCounts.get(idx);
                int sx = x + AccessTerminalLayout.NETWORK_X + col * AccessTerminalLayout.SLOT_SIZE + 1;
                int sy = y + AccessTerminalLayout.NETWORK_Y + row * AccessTerminalLayout.SLOT_SIZE + 1;

                graphics.renderItem(stack, sx, sy);

                if (count > 1) {
                    graphics.renderItemDecorations(font, stack, sx, sy, abbreviateCount(count));
                } else {
                    graphics.renderItemDecorations(font, stack, sx, sy, null);
                }
            }
        }
    }

    private void drawScrollbar(GuiGraphics graphics, int x, int y) {
        int barX = x + AccessTerminalLayout.SCROLLBAR_X;
        int barY = y + AccessTerminalLayout.NETWORK_Y;
        int barH = AccessTerminalLayout.NETWORK_H;

        // Track
        graphics.fill(barX, barY, barX + AccessTerminalLayout.SCROLLBAR_W, barY + barH, 0x40000000);

        // Thumb
        int totalRows = networkStacks == null
                ? 1
                : Math.max(
                        (networkStacks.size() + AccessTerminalLayout.NETWORK_COLS - 1)
                                / AccessTerminalLayout.NETWORK_COLS,
                        1);
        int thumbH = Math.max(8, barH * AccessTerminalLayout.NETWORK_VISIBLE_ROWS / totalRows);
        int maxScroll = Math.max(1, totalRows - AccessTerminalLayout.NETWORK_VISIBLE_ROWS);
        int thumbY = barY + (barH - thumbH) * scrollOffset / maxScroll;
        if (totalRows <= AccessTerminalLayout.NETWORK_VISIBLE_ROWS) {
            thumbY = barY;
            thumbH = barH;
        }
        graphics.fill(barX, thumbY, barX + AccessTerminalLayout.SCROLLBAR_W, thumbY + thumbH, 0xC0FFFFFF);
    }

    private void drawCraftingSection(GuiGraphics graphics, int x, int y) {
        // 3×3 crafting grid
        for (int row = 0; row < AccessTerminalLayout.CRAFTING_ROWS; row++) {
            for (int col = 0; col < AccessTerminalLayout.CRAFTING_ROWS; col++) {
                drawSlotBackground(
                        graphics,
                        x + AccessTerminalLayout.CRAFTING_GRID_X + col * AccessTerminalLayout.SLOT_SIZE,
                        y + AccessTerminalLayout.CRAFTING_Y + row * AccessTerminalLayout.SLOT_SIZE,
                        16,
                        16);
            }
        }

        // Arrow — centred vertically on the crafting rows
        int arrowDestY = y
                + AccessTerminalLayout.CRAFTING_Y
                + (AccessTerminalLayout.CRAFTING_H - AccessTerminalLayout.ARROW_H) / 2;
        graphics.blit(
                CRAFTING_TEXTURE,
                x + AccessTerminalLayout.ARROW_X,
                arrowDestY,
                AccessTerminalLayout.ARROW_SRC_X,
                AccessTerminalLayout.ARROW_SRC_Y,
                AccessTerminalLayout.ARROW_W,
                AccessTerminalLayout.ARROW_H);

        // Result slot
        drawSlotBackground(
                graphics,
                x + AccessTerminalLayout.CRAFTING_RESULT_X,
                y + AccessTerminalLayout.CRAFTING_RESULT_Y,
                16,
                16);
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
        graphics.drawString(font, title, (AccessTerminalLayout.BG_WIDTH - font.width(title)) / 2, 4, 0x404040, false);
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
