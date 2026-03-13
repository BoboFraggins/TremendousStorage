package net.bobofraggins.intellistore.storage.wirelesshub;

import net.bobofraggins.intellistore.shared.ui.Dialog;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Wireless Hub block.
 *
 * <p>Simple two-slot UI (176 × 166 px, standard inventory height):
 * <ul>
 *   <li>Left slot (slot 0): input — place unlinked Wireless SAT here
 *   <li>Right slot (slot 1): output — retrieve linked Wireless SAT from here
 * </ul>
 * An arrow between the slots indicates the linking direction.
 */
public class WirelessHubScreen extends AbstractContainerScreen<WirelessHubMenu> {

    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 166;

    private final Dialog dialog;

    public WirelessHubScreen(WirelessHubMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        dialog = new Dialog(Dialog.blankPane(BG_WIDTH, BG_HEIGHT - Dialog.TITLE_H - Dialog.BOTTOM_PADDING));
        this.imageWidth = dialog.totalWidth();
        this.imageHeight = dialog.totalHeight();
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;

        dialog.render(graphics, font, title, mouseX, mouseY, partialTick);

        // Slot backgrounds (inset style: dark top-left border, light bottom-right, gray fill)
        drawSlotBg(graphics, x + 62, y + 35); // input
        drawSlotBg(graphics, x + 98, y + 35); // output

        // Arrow between slots (→)
        graphics.fill(x + 80, y + 43, x + 96, y + 47, 0xFF888888);

        // Separator above player inventory
        graphics.fill(x + 4, y + 81, x + BG_WIDTH - 4, y + 82, 0xFF555555);
        graphics.fill(x + 4, y + 82, x + BG_WIDTH - 4, y + 83, 0xFFFFFFFF);

        // Player inventory slot backgrounds
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBg(graphics, x + 8 + col * 18, y + 84 + row * 18);
            }
        }
        // Hotbar slot backgrounds
        for (int col = 0; col < 9; col++) {
            drawSlotBg(graphics, x + 8 + col * 18, y + 142);
        }

        // "Unlinked" / "Linked" labels
        graphics.drawString(
                font,
                Component.translatable("screen.intellistore.wireless_hub.unlinked"),
                x + 62,
                y + 55,
                0x404040,
                false);
        graphics.drawString(
                font,
                Component.translatable("screen.intellistore.wireless_hub.linked"),
                x + 98,
                y + 55,
                0x006600,
                false);
    }

    /** Draws a standard 16×16 inset slot background at the given top-left pixel. */
    private static void drawSlotBg(GuiGraphics graphics, int x, int y) {
        // Dark top and left edges
        graphics.fill(x - 1, y - 1, x + 16, y, 0xFF373737);
        graphics.fill(x - 1, y - 1, x, y + 16, 0xFF373737);
        // Light bottom and right edges
        graphics.fill(x - 1, y + 16, x + 17, y + 17, 0xFFFFFFFF);
        graphics.fill(x + 16, y - 1, x + 17, y + 17, 0xFFFFFFFF);
        // Gray fill
        graphics.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title is drawn by Dialog.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
