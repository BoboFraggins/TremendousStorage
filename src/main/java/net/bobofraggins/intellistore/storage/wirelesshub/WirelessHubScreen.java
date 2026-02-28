package net.bobofraggins.intellistore.storage.wirelesshub;

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

    public WirelessHubScreen(WirelessHubMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Overall background
        graphics.fill(leftPos, topPos, leftPos + BG_WIDTH, topPos + BG_HEIGHT, 0xC0101010);

        // Title
        Component title = Component.translatable("screen.intellistore.wireless_hub");
        graphics.drawString(font, title, leftPos + (BG_WIDTH - font.width(title)) / 2, topPos + 6, 0xFFFFFF, false);

        // Slot backgrounds
        graphics.fill(leftPos + 61, topPos + 34, leftPos + 79, topPos + 52, 0x40FFFFFF); // input
        graphics.fill(leftPos + 97, topPos + 34, leftPos + 115, topPos + 52, 0x40FFFFFF); // output

        // Arrow between slots (→)
        graphics.fill(leftPos + 80, topPos + 41, leftPos + 96, topPos + 46, 0x80AAAAAA);

        // Separator above player inventory
        graphics.fill(leftPos + 4, topPos + 81, leftPos + BG_WIDTH - 4, topPos + 82, 0x80FFFFFF);

        // Player inventory slot backgrounds
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = leftPos + 8 + col * 18 - 1;
                int sy = topPos + 84 + row * 18 - 1;
                graphics.fill(sx, sy, sx + 18, sy + 18, 0x40FFFFFF);
            }
        }
        // Hotbar slot backgrounds
        for (int col = 0; col < 9; col++) {
            int sx = leftPos + 8 + col * 18 - 1;
            int sy = topPos + 142 - 1;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0x50FFFFFF);
        }

        // "Unlinked → Linked" labels
        graphics.drawString(
                font,
                Component.translatable("screen.intellistore.wireless_hub.unlinked"),
                leftPos + 62,
                topPos + 55,
                0xAAAAAA,
                false);
        graphics.drawString(
                font,
                Component.translatable("screen.intellistore.wireless_hub.linked"),
                leftPos + 98,
                topPos + 55,
                0x55FF55,
                false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Labels drawn in renderBg to avoid default white background label; suppress default here
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
