package net.bobofraggins.intellistore.ui;

import net.bobofraggins.intellistore.network.SetStorageInterfacePriorityPacket;
import net.bobofraggins.intellistore.priority.Priority;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Priority screen for a Storage Interface attachment on a Tube face.
 *
 * <p>Background panel: 176 × 60 pixels. Shows block name and a 5-button priority row.
 */
public class StorageInterfaceScreen extends AbstractContainerScreen<StorageInterfaceMenu> {

    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 60;

    private static final int PRIORITY_Y_OFFSET = 30;
    private static final int PRIORITY_BUTTON_W = 28;
    private static final int PRIORITY_BUTTON_H = 20;
    private static final int PRIORITY_BUTTON_GAP = 2;

    public StorageInterfaceScreen(StorageInterfaceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        buildPriorityButtons();
    }

    private void buildPriorityButtons() {
        int totalW = Priority.VALUES.length * PRIORITY_BUTTON_W + (Priority.VALUES.length - 1) * PRIORITY_BUTTON_GAP;
        int startX = leftPos + (BG_WIDTH - totalW) / 2;
        int btnY = topPos + PRIORITY_Y_OFFSET;

        for (int i = 0; i < Priority.VALUES.length; i++) {
            final int ordinal = i;
            Priority p = Priority.VALUES[i];
            addRenderableWidget(Button.builder(
                            Component.translatable(p.translationKey()),
                            btn -> PacketDistributor.sendToServer(
                                    new SetStorageInterfacePriorityPacket(menu.getPos(), menu.getFaceIndex(), ordinal)))
                    .bounds(
                            startX + i * (PRIORITY_BUTTON_W + PRIORITY_BUTTON_GAP),
                            btnY,
                            PRIORITY_BUTTON_W,
                            PRIORITY_BUTTON_H)
                    .build());
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int selected = menu.getPriority();
        for (int i = 0; i < Priority.VALUES.length; i++) {
            if (i < renderables.size() && renderables.get(i) instanceof Button btn) {
                btn.active = (i != selected);
            }
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
        Component label = Component.translatable("screen.intellistore.priority_label");
        graphics.drawString(
                font,
                label,
                leftPos + (BG_WIDTH - font.width(label)) / 2,
                topPos + PRIORITY_Y_OFFSET - 10,
                0xFFFFFF,
                false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (BG_WIDTH - font.width(title)) / 2, 6, 0xFFFFFF, false);
    }
}
