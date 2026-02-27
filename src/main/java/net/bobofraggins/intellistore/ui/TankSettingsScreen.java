package net.bobofraggins.intellistore.ui;

import net.bobofraggins.intellistore.network.SetVoidExcessPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Settings screen shared by Fluid Tank, Gas Tank, and Source Tank.
 *
 * <p>Shows a single "Void Excess" toggle button. Background panel: 176 × 60 pixels.
 */
public class TankSettingsScreen extends AbstractContainerScreen<TankSettingsMenu> {

    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 60;
    private static final int VOID_Y_OFFSET = 28;

    public TankSettingsScreen(TankSettingsMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int btnX = leftPos + (BG_WIDTH - 120) / 2;
        int btnY = topPos + VOID_Y_OFFSET;
        addRenderableWidget(Button.builder(voidExcessLabel(), btn -> {
                    boolean newValue = !menu.isVoidExcess();
                    PacketDistributor.sendToServer(new SetVoidExcessPacket(menu.getPos(), newValue));
                    btn.setMessage(voidExcessLabel(!menu.isVoidExcess()));
                })
                .bounds(btnX, btnY, 120, 20)
                .build());
    }

    private Component voidExcessLabel() {
        return voidExcessLabel(menu.isVoidExcess());
    }

    private Component voidExcessLabel(boolean value) {
        return Component.translatable(
                value ? "screen.intellistore.void_excess_on" : "screen.intellistore.void_excess_off");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!renderables.isEmpty() && renderables.get(0) instanceof Button btn) {
            btn.setMessage(voidExcessLabel());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + BG_WIDTH, topPos + BG_HEIGHT, 0xC0101010);
        Component label = Component.translatable("screen.intellistore.void_excess_label");
        graphics.drawString(
                font,
                label,
                leftPos + (BG_WIDTH - font.width(label)) / 2,
                topPos + VOID_Y_OFFSET - 10,
                0xFFFFFF,
                false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (BG_WIDTH - font.width(title)) / 2, 6, 0xFFFFFF, false);
    }
}
