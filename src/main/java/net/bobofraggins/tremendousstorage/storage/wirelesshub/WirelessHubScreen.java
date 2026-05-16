package net.bobofraggins.tremendousstorage.storage.wirelesshub;

import javax.annotation.Nullable;
import net.bobofraggins.tremendousstorage.shared.ui.Dialog;
import net.bobofraggins.tremendousstorage.shared.ui.IDialogPane;
import net.bobofraggins.tremendousstorage.shared.ui.PlayerInventoryPane;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 166;

    // Slot positions — same as Tank settings for visual consistency
    private static final int SLOT_X = 80;
    private static final int INPUT_Y = 20;
    private static final int OUTPUT_Y = 62;

    private final Dialog dialog;
    private final PlayerInventoryPane playerInvPane = new PlayerInventoryPane(7);

    /** Non-null only when the HAARP upgrade is active; handles weather-mode radio button clicks. */
    @Nullable
    private final HaarpWeatherPane haarpPane;

    public WirelessHubScreen(WirelessHubMenu menu, Inventory inv, Component title) {
        HaarpWeatherPane haarpPane_;
        Dialog dialog_;
        if (menu.hasHaarpUpgrade()) {
            haarpPane_ = new HaarpWeatherPane(menu::getHaarpModeOrdinal, menu::getHubPos);
            IDialogPane topBlank = Dialog.blankPane(BG_WIDTH, 64);
            IDialogPane botBlank = Dialog.blankPane(BG_WIDTH, BG_HEIGHT - 17 - 64 - 5);
            dialog_ = new Dialog(topBlank, haarpPane_, botBlank);
        } else {
            haarpPane_ = null;
            dialog_ = new Dialog(Dialog.blankPane(BG_WIDTH, BG_HEIGHT - Dialog.TITLE_H - Dialog.BOTTOM_PADDING));
        }
        super(menu, inv, title, dialog_.totalWidth(), dialog_.totalHeight());
        haarpPane = haarpPane_;
        dialog = dialog_;
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = leftPos, y = topPos;
        boolean haarp = menu.hasHaarpUpgrade();
        int haarpH = haarp ? WirelessHubMenu.HAARP_SECTION_H : 0;

        dialog.render(g, font, title, mouseX, mouseY, partialTick);

        // Slot backgrounds — input (top) and output (bottom), centred like the Tank
        drawSlot(g, x + SLOT_X, y + INPUT_Y);
        drawSlot(g, x + SLOT_X, y + OUTPUT_Y);

        // Down-arrow between the two slots
        drawDownArrow(g, x + SLOT_X, y + INPUT_Y + 16);

        // "Unlinked" / "Linked" labels — to the right of each slot, vertically centred
        Component unlinked = Component.translatable("screen.tremendousstorage.wireless_hub.unlinked");
        Component linked = Component.translatable("screen.tremendousstorage.wireless_hub.linked");
        int labelX = x + SLOT_X + 18;
        g.text(font, unlinked, labelX, y + INPUT_Y + (16 - font.lineHeight) / 2, 0x404040, false);
        g.text(font, linked, labelX, y + OUTPUT_Y + (16 - font.lineHeight) / 2, 0x006600, false);

        // Separator above HAARP section (or above player inventory if no HAARP)
        g.fill(x + 4, y + 81, x + BG_WIDTH - 4, y + 82, 0xFF555555);
        g.fill(x + 4, y + 82, x + BG_WIDTH - 4, y + 83, 0xFFFFFFFF);

        // Separator below HAARP section (only if upgrade active)
        if (haarp) {
            int sepY = y + 81 + haarpH;
            g.fill(x + 4, sepY, x + BG_WIDTH - 4, sepY + 1, 0xFF555555);
            g.fill(x + 4, sepY + 1, x + BG_WIDTH - 4, sepY + 2, 0xFFFFFFFF);
        }

        // Player inventory slot backgrounds
        int invPaneY = WirelessHubMenu.INV_Y_BASE + haarpH;
        g.pose().pushMatrix();
        g.pose().translate(x, y + invPaneY);
        playerInvPane.render(g, font, BG_WIDTH, mouseX - x, mouseY - (y + invPaneY), partialTick);
        g.pose().popMatrix();
        super.extractContents(g, mouseX, mouseY, partialTick);
    }

    private static void drawSlot(GuiGraphicsExtractor g, int sx, int sy) {
        g.fill(sx, sy, sx + 16, sy + 1, 0xFF373737); // top
        g.fill(sx, sy + 1, sx + 1, sy + 16, 0xFF373737); // left
        g.fill(sx, sy + 16, sx + 17, sy + 17, 0xFFFFFFFF); // bottom
        g.fill(sx + 16, sy, sx + 17, sy + 16, 0xFFFFFFFF); // right
        g.fill(sx + 1, sy + 1, sx + 16, sy + 16, 0xFF8B8B8B); // interior
    }

    private static void drawDownArrow(GuiGraphicsExtractor g, int gapX, int gapY) {
        int cx = gapX + 8;
        int top = gapY + 3;
        g.fill(cx - 1, top, cx + 1, top + 9, 0xFF555555); // stem
        g.fill(cx - 4, top + 9, cx + 4, top + 11, 0xFF555555); // arrowhead row 1
        g.fill(cx - 2, top + 11, cx + 2, top + 13, 0xFF555555); // arrowhead row 2
        g.fill(cx - 1, top + 13, cx + 1, top + 15, 0xFF555555); // arrowhead row 3
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // Title is drawn by Dialog.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        extractBackground(g, mouseX, mouseY, partialTick);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean consumed) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (dialog.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(event, consumed);
    }
}
