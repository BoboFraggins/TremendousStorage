package net.bobofraggins.tremendousstorage.storage.wirelesshub;

import net.bobofraggins.tremendousstorage.shared.ui.Dialog;
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

        // Slot backgrounds — input (left) and output (right), spread apart with room for labels
        drawSlotBg(graphics, x + 44, y + 35); // input  (slot centre x = 52)
        drawSlotBg(graphics, x + 120, y + 35); // output (slot centre x = 128)

        // Arrow between slots (→): 5px margins from each slot border, shaft + arrowhead
        graphics.fill(x + 66, y + 42, x + 106, y + 46, 0xFF888888); // shaft
        graphics.fill(x + 106, y + 40, x + 108, y + 48, 0xFF888888); // arrowhead

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

        // "Unlinked" / "Linked" labels — centred under their respective slots
        Component unlinked = Component.translatable("screen.tremendousstorage.wireless_hub.unlinked");
        Component linked = Component.translatable("screen.tremendousstorage.wireless_hub.linked");
        graphics.drawString(font, unlinked, x + 52 - font.width(unlinked) / 2, y + 55, 0x404040, false);
        graphics.drawString(font, linked, x + 128 - font.width(linked) / 2, y + 55, 0x006600, false);
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
