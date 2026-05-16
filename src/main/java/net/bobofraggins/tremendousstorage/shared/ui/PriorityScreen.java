package net.bobofraggins.tremendousstorage.shared.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Priority screen for storage blocks.
 *
 * <p>Hosts a single {@link PriorityControl} pane inside a {@link Dialog}.
 */
public class PriorityScreen extends AbstractContainerScreen<PriorityControl> {

    private Dialog dialog;

    public PriorityScreen(PriorityControl menu, Inventory inv, Component title) {
        Dialog dialog_ = new Dialog(menu);
        super(menu, inv, title, dialog_.totalWidth(), dialog_.totalHeight());
        dialog = dialog_;
    }

    @Override
    protected void init() {
        super.init();
        dialog.init(leftPos, topPos);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractBackground(graphics, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        dialog.render(graphics, font, title, mouseX, mouseY, partialTick);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Dialog draws the title
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
