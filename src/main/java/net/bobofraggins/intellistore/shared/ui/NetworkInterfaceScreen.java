package net.bobofraggins.intellistore.shared.ui;

import java.util.List;
import net.bobofraggins.intellistore.shared.network.NetworkContentsPacket;
import net.bobofraggins.intellistore.shared.network.RequestNetworkContentsPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen for the Network Interface block.
 *
 * <p>Layout (176 × 220 px):
 * <ul>
 *   <li>Title row + validity indicator
 *   <li>Scrollable list of attached IntelliStore blocks (translation keys → display names)
 * </ul>
 *
 * <p>When the screen opens it sends a {@link RequestNetworkContentsPacket} to the server;
 * the response populates {@link NetworkContentsPacket#PENDING} which is polled each tick.
 */
public class NetworkInterfaceScreen extends AbstractContainerScreen<NetworkInterfaceMenu> {

    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 220;

    /** Number of list rows visible at once. */
    private static final int VISIBLE_ROWS = 12;

    private static final int ROW_HEIGHT = 12;
    private static final int LIST_Y_START = 30;

    private List<String> entries = List.of();
    private int scrollOffset = 0;

    public NetworkInterfaceScreen(NetworkInterfaceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        // Request block list from server
        PacketDistributor.sendToServer(new RequestNetworkContentsPacket(menu.getPos()));
        // Clear stale data from a previous screen open
        NetworkContentsPacket.PENDING = List.of();
        entries = List.of();
        scrollOffset = 0;
    }

    // -------------------------------------------------------------------------
    // Tick — poll for server response
    // -------------------------------------------------------------------------

    @Override
    protected void containerTick() {
        super.containerTick();
        List<String> pending = NetworkContentsPacket.PENDING;
        if (!pending.isEmpty() && pending != entries) {
            entries = pending;
            // Clamp scroll after receiving new data
            clampScroll();
        }
    }

    // -------------------------------------------------------------------------
    // Scroll
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= (int) Math.signum(scrollY);
        clampScroll();
        return true;
    }

    private void clampScroll() {
        int maxScroll = Math.max(0, entries.size() - VISIBLE_ROWS);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Dark background panel
        graphics.fill(leftPos, topPos, leftPos + BG_WIDTH, topPos + BG_HEIGHT, 0xC0101010);

        // Validity indicator line below title
        boolean valid = menu.isNetworkValid();
        Component statusText = Component.translatable(
                valid
                        ? "screen.intellistore.network_interface.valid"
                        : "screen.intellistore.network_interface.invalid");
        int statusColor = valid ? 0x55FF55 : 0xFF5555;
        graphics.drawString(
                font, statusText, leftPos + (BG_WIDTH - font.width(statusText)) / 2, topPos + 18, statusColor, false);

        // List area separator
        graphics.fill(
                leftPos + 4, topPos + LIST_Y_START - 2, leftPos + BG_WIDTH - 4, topPos + LIST_Y_START - 1, 0x80FFFFFF);

        // Draw list rows
        int listAreaBottom = topPos + LIST_Y_START + VISIBLE_ROWS * ROW_HEIGHT;
        graphics.enableScissor(leftPos + 4, topPos + LIST_Y_START, leftPos + BG_WIDTH - 4, listAreaBottom);

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx = i + scrollOffset;
            if (idx >= entries.size()) break;

            String key = entries.get(idx);
            // Translate if it looks like a translation key; otherwise display as-is
            String displayStr;
            if (key.contains(".")) {
                displayStr = Component.translatable(key).getString();
            } else {
                displayStr = key;
            }

            int rowY = topPos + LIST_Y_START + i * ROW_HEIGHT;
            graphics.drawString(font, displayStr, leftPos + 6, rowY, 0xE0E0E0, false);
        }

        graphics.disableScissor();

        // Empty state
        if (entries.isEmpty()) {
            String emptyMsg = "No blocks connected";
            graphics.drawString(
                    font,
                    emptyMsg,
                    leftPos + (BG_WIDTH - font.width(emptyMsg)) / 2,
                    topPos + LIST_Y_START + 4,
                    0x808080,
                    false);
        }

        // Scroll bar (if needed)
        if (entries.size() > VISIBLE_ROWS) {
            int barX = leftPos + BG_WIDTH - 8;
            int barY = topPos + LIST_Y_START;
            int barH = VISIBLE_ROWS * ROW_HEIGHT;
            graphics.fill(barX, barY, barX + 4, barY + barH, 0x40FFFFFF);

            int thumbH = Math.max(8, barH * VISIBLE_ROWS / entries.size());
            int thumbY = barY + (barH - thumbH) * scrollOffset / Math.max(1, entries.size() - VISIBLE_ROWS);
            graphics.fill(barX, thumbY, barX + 4, thumbY + thumbH, 0xC0FFFFFF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (BG_WIDTH - font.width(title)) / 2, 6, 0xFFFFFF, false);
    }
}
