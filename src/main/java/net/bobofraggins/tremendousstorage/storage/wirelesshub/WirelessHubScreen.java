package net.bobofraggins.tremendousstorage.storage.wirelesshub;

import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.ui.Dialog;
import net.bobofraggins.tremendousstorage.shared.ui.IDialogPane;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Wireless Hub block.
 *
 * <p>Base layout (176 × 166 px): two slots with a right-pointing arrow, labels, separator, and
 * player inventory. When the HAARP Upgrade is applied an additional 68 px section appears between
 * the linking-slots area and the player inventory.
 */
public class WirelessHubScreen extends AbstractContainerScreen<WirelessHubMenu> {

    private static final int BG_WIDTH  = 176;
    private static final int BG_HEIGHT = 166;

    private final Dialog dialog;

    /** Non-null only when the HAARP upgrade is active; handles weather-mode radio button clicks. */
    @Nullable
    private final HaarpWeatherPane haarpPane;

    public WirelessHubScreen(WirelessHubMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);

        if (menu.hasHaarpUpgrade()) {
            haarpPane = new HaarpWeatherPane(menu::getHaarpModeOrdinal, menu::getHubPos);
            IDialogPane topBlank  = Dialog.blankPane(BG_WIDTH, 64);
            IDialogPane botBlank  = Dialog.blankPane(BG_WIDTH, BG_HEIGHT - 17 - 64 - 5);
            dialog = new Dialog(topBlank, haarpPane, botBlank);
        } else {
            haarpPane = null;
            dialog    = new Dialog(Dialog.blankPane(BG_WIDTH, BG_HEIGHT - Dialog.TITLE_H - Dialog.BOTTOM_PADDING));
        }

        this.imageWidth  = dialog.totalWidth();
        this.imageHeight = dialog.totalHeight();
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        boolean haarp = menu.hasHaarpUpgrade();
        int haarpH = haarp ? WirelessHubMenu.HAARP_SECTION_H : 0;

        dialog.render(g, font, title, mouseX, mouseY, partialTick);

        // Slot backgrounds — input (left) and output (right)
        drawSlotBg(g, x + 44,  y + 35);
        drawSlotBg(g, x + 120, y + 35);

        // Arrow between slots (→)
        g.fill(x + 66, y + 42, x + 106, y + 46, 0xFF888888); // shaft
        g.fill(x + 106, y + 40, x + 108, y + 48, 0xFF888888); // arrowhead

        // Separator above HAARP section (or above player inventory if no HAARP)
        g.fill(x + 4, y + 81, x + BG_WIDTH - 4, y + 82, 0xFF555555);
        g.fill(x + 4, y + 82, x + BG_WIDTH - 4, y + 83, 0xFFFFFFFF);

        // Separator below HAARP section (only if upgrade active)
        if (haarp) {
            int sepY = y + 81 + haarpH;
            g.fill(x + 4, sepY,     x + BG_WIDTH - 4, sepY + 1, 0xFF555555);
            g.fill(x + 4, sepY + 1, x + BG_WIDTH - 4, sepY + 2, 0xFFFFFFFF);
        }

        // Player inventory slot backgrounds
        int invY    = y + WirelessHubMenu.INV_Y_BASE    + haarpH;
        int hotbarY = y + WirelessHubMenu.HOTBAR_Y_BASE + haarpH;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBg(g, x + 8 + col * 18, invY + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBg(g, x + 8 + col * 18, hotbarY);
        }

        // "Unlinked" / "Linked" labels
        Component unlinked = Component.translatable("screen.tremendousstorage.wireless_hub.unlinked");
        Component linked   = Component.translatable("screen.tremendousstorage.wireless_hub.linked");
        g.drawString(font, unlinked, x + 52 - font.width(unlinked) / 2, y + 55, 0x404040, false);
        g.drawString(font, linked,   x + 128 - font.width(linked) / 2,  y + 55, 0x006600, false);
    }

    /** Draws a standard 16×16 inset slot background at the given top-left pixel. */
    private static void drawSlotBg(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 16, y,      0xFF373737);
        g.fill(x - 1, y - 1, x,      y + 16,  0xFF373737);
        g.fill(x - 1, y + 16, x + 17, y + 17, 0xFFFFFFFF);
        g.fill(x + 16, y - 1, x + 17, y + 17, 0xFFFFFFFF);
        g.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Title is drawn by Dialog.
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (dialog.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
