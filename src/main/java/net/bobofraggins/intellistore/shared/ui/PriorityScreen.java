package net.bobofraggins.intellistore.shared.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Priority screen for Junk Drawer and Bulk Storage Container.
 *
 * <p>Hosts a single {@link PriorityControl} pane inside a {@link Dialog}.
 */
public class PriorityScreen extends AbstractContainerScreen<PriorityControl> {

    private static final int BG_WIDTH = 176;

    private Dialog dialog;

    public PriorityScreen(PriorityControl menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = Dialog.TITLE_H + menu.preferredHeight();
    }

    @Override
    protected void init() {
        super.init();
        dialog = new Dialog(imageWidth, menu);
        dialog.init(leftPos, topPos);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        dialog.render(graphics, font, title, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Dialog draws the title
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (dialog.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
