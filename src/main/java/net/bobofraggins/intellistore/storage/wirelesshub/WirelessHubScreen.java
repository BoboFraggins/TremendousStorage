package net.bobofraggins.intellistore.storage.wirelesshub;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 166;

    public WirelessHubScreen(WirelessHubMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;

        // Title bar (top 17px of generic_54)
        graphics.blit(BG_TEXTURE, x, y, 0, 0, BG_WIDTH, 17);

        // Gray fill for the rest of the panel
        graphics.fill(x, y + 17, x + BG_WIDTH, y + BG_HEIGHT, 0xFFC6C6C6);

        // Left, right, and bottom border lines
        graphics.fill(x, y + 17, x + 1, y + BG_HEIGHT, 0xFF555555);
        graphics.fill(x + BG_WIDTH - 1, y + 17, x + BG_WIDTH, y + BG_HEIGHT, 0xFFFFFFFF);
        graphics.fill(x, y + BG_HEIGHT - 1, x + BG_WIDTH, y + BG_HEIGHT, 0xFF555555);

        // Title
        Component title = Component.translatable("screen.intellistore.wireless_hub");
        graphics.drawString(font, title, x + (BG_WIDTH - font.width(title)) / 2, y + 6, 0x404040, false);

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
        // Labels drawn in renderBg to avoid default white background label; suppress default here
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
