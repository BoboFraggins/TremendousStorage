package net.bobofraggins.intellistore.shared.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
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
        graphics.fill(leftPos, topPos, leftPos + BG_WIDTH, topPos + imageHeight, 0xC0101010);

        for (int i = 0; i < AbstractFilingCabinetMenu.FOLDER_SLOTS; i++) {
            int col = i % 4;
            int row = i / 4;
            int sx = leftPos + 29 + col * 18;
            int sy = topPos + 44 + row * 18;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF303030);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF1A1A1A);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (BG_WIDTH - font.width(title)) / 2, 6, 0xFFFFFF, false);
    }
}
