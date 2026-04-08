package net.bobofraggins.tremendousstorage.storage.recyclingbin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class RecyclingBinScreen extends AbstractContainerScreen<RecyclingBinMenu> {

    // Layout constants matching RecyclingBinMenu slot positions
    private static final int IMG_WIDTH = 176;
    private static final int IMG_HEIGHT = 133;

    public RecyclingBinScreen(RecyclingBinMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = IMG_WIDTH;
        imageHeight = IMG_HEIGHT;
        inventoryLabelY = IMG_HEIGHT - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // Outer background
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);

        // Title bar separator
        graphics.fill(x, y + 14, x + imageWidth, y + 15, 0xFF555555);

        // Slot divider above player inventory
        graphics.fill(x, y + 43, x + imageWidth, y + 44, 0xFF555555);

        // Void slot indicator (dark inset around the single slot)
        int sx = x + RecyclingBinMenu.VOID_SLOT_X;
        int sy = y + RecyclingBinMenu.VOID_SLOT_Y;
        graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
        graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }
}
