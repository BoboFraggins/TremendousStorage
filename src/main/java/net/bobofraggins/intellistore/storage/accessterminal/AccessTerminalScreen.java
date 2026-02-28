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
 * <p>Layout (176 × 256 px):
 * <ul>
 *   <li>y=0..8   title bar
 *   <li>y=8..14  network status (green "Connected" / orange "Not Connected")
 *   <li>y=14..16 separator
 *   <li>y=16..128 scrollable network item list (7 visible rows × 16px each)
 *   <li>y=128..130 separator
 *   <li>y=130..166 3×3 crafting grid (left) + result slot (right)
 *   <li>y=166..168 separator
 *   <li>y=168..256 player inventory + hotbar
 * </ul>
 *
 * <p>Left-click a network row → extracts one stack (up to maxStackSize) into player inventory.
 * Shift-left-click a network row → extracts the full count (up to maxStackSize) at once.
 * Shift-left-click a player inventory slot → inserts that slot's stack into the network.
 */
public class AccessTerminalScreen extends AbstractContainerScreen<AccessTerminalMenu> {

    // -------------------------------------------------------------------------
    // Layout constants
    // -------------------------------------------------------------------------

    private static final int BG_WIDTH = 176;

    // Vanilla generic chest texture — 176 wide, top 17px = title bar, bottom 96px = player inv
    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int LIST_Y = 16;
    private static final int LIST_HEIGHT = 112; // 7 rows × 16 px
    private static final int ROW_HEIGHT = 16;
    private static final int VISIBLE_ROWS = 7;

    // CRAFT_Y: separator + 2px padding below list area
    private static final int CRAFT_Y = LIST_Y + LIST_HEIGHT + 4; // 132
    // INV_Y: 3 craft rows × 18px + 4px gap below craft area
    private static final int INV_Y = CRAFT_Y + 3 * 18 + 4; // 190
    // HOTBAR_Y: 3 inv rows × 18px + 4px gap
    private static final int HOTBAR_Y = INV_Y + 3 * 18 + 4; // 248

    private static final int BG_HEIGHT = HOTBAR_Y + 18 + 4; // 270

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private List<ItemStack> networkStacks = List.of();
    private List<Long> networkCounts = List.of();
    private int scrollOffset = 0;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public AccessTerminalScreen(AccessTerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    // -------------------------------------------------------------------------
    // Init — send request, position slots
    // -------------------------------------------------------------------------

    @Override
    protected void init() {
        super.init();

        // Request network contents
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
    // Tick — poll for server response
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
        return mx >= leftPos + 4
                && mx < leftPos + BG_WIDTH - 12
                && my >= topPos + LIST_Y
                && my < topPos + LIST_Y + LIST_HEIGHT;
    }

    // -------------------------------------------------------------------------
    // Mouse click — extract from network or insert from player inv
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInListArea(mouseX, mouseY)) {
            int relY = (int) (mouseY - topPos - LIST_Y);
            int rowIndex = relY / ROW_HEIGHT + scrollOffset;
            if (rowIndex >= 0 && rowIndex < networkStacks.size() && menu.hasNetwork()) {
                ItemStack target = networkStacks.get(rowIndex);
                long totalCount = networkCounts.get(rowIndex);
                boolean shift = hasShiftDown();

                int amount = shift
                        ? (int) Math.min(totalCount, target.getMaxStackSize())
                        : Math.min((int) totalCount, target.getMaxStackSize());

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

        // Tooltip for hovered network row
        if (isInListArea(mouseX, mouseY)) {
            int relY = (int) (mouseY - topPos - LIST_Y);
            int rowIndex = relY / ROW_HEIGHT + scrollOffset;
            if (rowIndex >= 0 && rowIndex < networkStacks.size()) {
                graphics.renderTooltip(font, networkStacks.get(rowIndex), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Background: blit the vanilla generic-chest texture.
        // It is 176×222 px: top 17px = title row, bottom 96px = player inv section.
        // We blit the title row at the top, then a plain fill for the list area,
        // then the bottom player-inv slice aligned to our INV_Y.
        int x = leftPos, y = topPos;

        // Title bar (top 17 rows of the texture)
        graphics.blit(BG_TEXTURE, x, y, 0, 0, BG_WIDTH, 17);

        // Player inventory section from the texture.
        // In generic_54.png the slot backgrounds start at src y=143 (17px into the inv section).
        // Blit so that src y=143 lands at screen y=topPos+INV_Y, i.e. dest y = topPos+INV_Y-17.
        int invBgY = y + INV_Y - 17;

        // Middle fill (between title bar and player inventory background)
        int midTop = y + 17;
        int midBottom = invBgY;
        if (midBottom > midTop) {
            graphics.fill(x, midTop, x + BG_WIDTH, midBottom, 0xFFC6C6C6);
            // Border lines to match vanilla panel look
            graphics.fill(x, midTop, x + 1, midBottom, 0xFF555555); // left
            graphics.fill(x + BG_WIDTH - 1, midTop, x + BG_WIDTH, midBottom, 0xFFFFFFFF); // right
        }

        // Player inventory section (96px tall, src y=126 in generic_54.png)
        graphics.blit(BG_TEXTURE, x, invBgY, 0, 126, BG_WIDTH, 96);

        // Network status line
        boolean connected = menu.hasNetwork();
        String statusKey = connected
                ? "screen.intellistore.access_terminal.connected"
                : "screen.intellistore.access_terminal.disconnected";
        Component statusText = Component.translatable(statusKey);
        int statusColor = connected ? 0x55FF55 : 0xFF9955;
        graphics.drawString(
                font, statusText, leftPos + (BG_WIDTH - font.width(statusText)) / 2, topPos + 9, statusColor, false);

        // Separator below status
        graphics.fill(leftPos + 4, topPos + LIST_Y - 2, leftPos + BG_WIDTH - 4, topPos + LIST_Y - 1, 0x80FFFFFF);

        // Network item list
        int listBottom = topPos + LIST_Y + LIST_HEIGHT;
        graphics.enableScissor(leftPos + 4, topPos + LIST_Y, leftPos + BG_WIDTH - 12, listBottom);

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx = i + scrollOffset;
            if (idx >= networkStacks.size()) break;

            ItemStack stack = networkStacks.get(idx);
            long count = networkCounts.get(idx);
            int rowX = leftPos + 6;
            int rowY = topPos + LIST_Y + i * ROW_HEIGHT;

            // Item icon
            graphics.renderItem(stack, rowX, rowY);
            graphics.renderItemDecorations(font, stack, rowX, rowY, null);

            // Item name
            String name = stack.getDisplayName().getString();
            graphics.drawString(font, name, rowX + 20, rowY + 4, 0xE0E0E0, false);

            // Count (abbreviated for large numbers)
            String countStr = abbreviateCount(count);
            int countX = leftPos + BG_WIDTH - 14 - font.width(countStr);
            graphics.drawString(font, countStr, countX, rowY + 4, 0xAAAAAA, false);
        }

        graphics.disableScissor();

        if (networkStacks.isEmpty() && connected) {
            Component empty = Component.translatable("screen.intellistore.access_terminal.empty");
            graphics.drawString(
                    font, empty, leftPos + (BG_WIDTH - font.width(empty)) / 2, topPos + LIST_Y + 4, 0x808080, false);
        } else if (!connected) {
            Component noNet = Component.translatable("screen.intellistore.access_terminal.disconnected");
            graphics.drawString(
                    font, noNet, leftPos + (BG_WIDTH - font.width(noNet)) / 2, topPos + LIST_Y + 4, 0x808080, false);
        }

        // Scroll bar
        if (networkStacks.size() > VISIBLE_ROWS) {
            int barX = leftPos + BG_WIDTH - 10;
            int barY = topPos + LIST_Y;
            int barH = LIST_HEIGHT;
            graphics.fill(barX, barY, barX + 4, barY + barH, 0x40FFFFFF);
            int thumbH = Math.max(8, barH * VISIBLE_ROWS / networkStacks.size());
            int thumbY = barY + (barH - thumbH) * scrollOffset / Math.max(1, networkStacks.size() - VISIBLE_ROWS);
            graphics.fill(barX, thumbY, barX + 4, thumbY + thumbH, 0xC0FFFFFF);
        }

        // Separator above crafting
        graphics.fill(leftPos + 4, topPos + CRAFT_Y - 2, leftPos + BG_WIDTH - 4, topPos + CRAFT_Y - 1, 0x80FFFFFF);

        // Crafting slot backgrounds — vanilla inset style (dark border + slightly darker fill)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = leftPos + 30 + col * 18;
                int sy = topPos + CRAFT_Y + row * 18;
                graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737); // dark border
                graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B); // slot interior
            }
        }
        // Result slot background
        graphics.fill(leftPos + 123, topPos + CRAFT_Y + 8, leftPos + 139, topPos + CRAFT_Y + 24, 0xFF373737);
        graphics.fill(leftPos + 124, topPos + CRAFT_Y + 9, leftPos + 140, topPos + CRAFT_Y + 25, 0xFF8B8B8B);

        // Arrow between grid and result
        graphics.fill(leftPos + 97, topPos + CRAFT_Y + 13, leftPos + 121, topPos + CRAFT_Y + 20, 0xFF888888);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (BG_WIDTH - font.width(title)) / 2, 1, 0xFFFFFF, false);
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
