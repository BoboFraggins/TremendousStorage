package net.bobofraggins.intellistore.shared.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Shared screen base for
 * {@link net.bobofraggins.intellistore.storage.filingcabinet.FilingCabinetScreen} and
 * {@link net.bobofraggins.intellistore.storage.personalfilingcabinet.PersonalFilingCabinetScreen}.
 *
 * <p>Handles the void-excess toggle button, folder-slot grid rendering, title label,
 * and the standard render pipeline. Subclasses supply the background height and the
 * action to fire when the void-excess button is clicked.
 */
public abstract class AbstractFilingCabinetScreen<M extends AbstractFilingCabinetMenu>
        extends AbstractContainerScreen<M> {

    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    protected static final int BG_WIDTH = 176;
    private static final int BTN_W = 120;
    private static final int BTN_H = 14;
    private static final int BTN_Y = 18;

    private Button voidExcessButton;

    protected AbstractFilingCabinetScreen(M menu, Inventory inv, Component title, int bgHeight) {
        super(menu, inv, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = bgHeight;
    }

    /**
     * Returns the action to run when the void-excess button is clicked.
     * Implementations should toggle {@code menu.isVoidExcess()}, call
     * {@code menu.setVoidExcess()}, and send the appropriate packet to the server.
     */
    protected abstract Button.OnPress voidExcessAction();

    @Override
    protected void init() {
        super.init();
        int btnX = leftPos + (BG_WIDTH - BTN_W) / 2;
        int btnY = topPos + BTN_Y;
        voidExcessButton = addRenderableWidget(Button.builder(voidExcessLabel(), voidExcessAction())
                .bounds(btnX, btnY, BTN_W, BTN_H)
                .build());
    }

    private Component voidExcessLabel() {
        return menu.isVoidExcess()
                ? Component.translatable("screen.intellistore.void_excess_on")
                : Component.translatable("screen.intellistore.void_excess_off");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (voidExcessButton != null) voidExcessButton.setMessage(voidExcessLabel());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;

        // Title bar (top 17px of generic_54)
        graphics.blit(BG_TEXTURE, x, y, 0, 0, BG_WIDTH, 17);

        // Gray fill for the rest of the panel
        graphics.fill(x, y + 17, x + BG_WIDTH, y + imageHeight, 0xFFC6C6C6);

        // Left and right border lines
        graphics.fill(x,               y + 17, x + 1,           y + imageHeight, 0xFF555555);
        graphics.fill(x + BG_WIDTH - 1, y + 17, x + BG_WIDTH,    y + imageHeight, 0xFFFFFFFF);

        // Bottom border
        graphics.fill(x, y + imageHeight - 1, x + BG_WIDTH, y + imageHeight, 0xFF555555);

        // Folder slots — vanilla inset style
        for (int i = 0; i < AbstractFilingCabinetMenu.FOLDER_SLOTS; i++) {
            int col = i % 4;
            int row = i / 4;
            int sx = x + 29 + col * 18;
            int sy = y + 44 + row * 18;
            drawSlotBackground(graphics, sx, sy, 16, 16);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (BG_WIDTH - font.width(title)) / 2, 4, 0x404040, false);
    }

    private static void drawSlotBackground(GuiGraphics graphics, int sx, int sy, int w, int h) {
        graphics.fill(sx,     sy,     sx + w, sy + 1, 0xFF373737); // top
        graphics.fill(sx,     sy + 1, sx + 1, sy + h, 0xFF373737); // left
        graphics.fill(sx,     sy + h, sx + w + 1, sy + h + 1, 0xFFFFFFFF); // bottom
        graphics.fill(sx + w, sy,     sx + w + 1, sy + h,     0xFFFFFFFF); // right
        graphics.fill(sx + 1, sy + 1, sx + w, sy + h, 0xFF8B8B8B); // interior
    }
}
