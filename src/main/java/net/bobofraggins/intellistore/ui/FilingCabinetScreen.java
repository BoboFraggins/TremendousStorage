package net.bobofraggins.intellistore.ui;

import net.bobofraggins.intellistore.network.SetPriorityPacket;
import net.bobofraggins.intellistore.network.ToggleFilingCabinetPacket;
import net.bobofraggins.intellistore.priority.Priority;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen for the Filing Cabinet. Shows an open/close toggle and a 5-button priority row.
 *
 * <p>Background panel: 176 × 96 pixels (standard chest width, shorter height).
 */
public class FilingCabinetScreen extends AbstractContainerScreen<FilingCabinetMenu> {

    // Background panel dimensions
    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 96;

    // Widget layout constants
    private static final int TOGGLE_Y_OFFSET = 20;
    private static final int PRIORITY_Y_OFFSET = 54;
    private static final int PRIORITY_BUTTON_W = 28;
    private static final int PRIORITY_BUTTON_H = 20;
    private static final int PRIORITY_BUTTON_GAP = 2;

    public FilingCabinetScreen(FilingCabinetMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        buildWidgets();
    }

    private void buildWidgets() {
        // Toggle button — centred, below the title
        int toggleX = leftPos + (BG_WIDTH - 80) / 2;
        int toggleY = topPos + TOGGLE_Y_OFFSET;
        addRenderableWidget(Button.builder(toggleLabel(), btn -> {
                    PacketDistributor.sendToServer(new ToggleFilingCabinetPacket(menu.getPos()));
                    // Rebuild buttons immediately so the label reflects the new state
                    // (ContainerData will sync on next tick)
                    rebuildToggle(btn);
                })
                .bounds(toggleX, toggleY, 80, 20)
                .build());

        // Priority buttons — 5 in a row, centred
        int totalW = Priority.VALUES.length * PRIORITY_BUTTON_W
                + (Priority.VALUES.length - 1) * PRIORITY_BUTTON_GAP;
        int startX = leftPos + (BG_WIDTH - totalW) / 2;
        int btnY = topPos + PRIORITY_Y_OFFSET;

        for (int i = 0; i < Priority.VALUES.length; i++) {
            final int ordinal = i;
            Priority p = Priority.VALUES[i];
            addRenderableWidget(Button.builder(Component.translatable(p.translationKey()), btn -> {
                        PacketDistributor.sendToServer(new SetPriorityPacket(menu.getPos(), ordinal));
                    })
                    .bounds(startX + i * (PRIORITY_BUTTON_W + PRIORITY_BUTTON_GAP), btnY, PRIORITY_BUTTON_W, PRIORITY_BUTTON_H)
                    .build());
        }
    }

    private Component toggleLabel() {
        return Component.translatable(
                menu.isOpen() ? "screen.intellistore.filing_cabinet.toggle_open"
                              : "screen.intellistore.filing_cabinet.toggle_closed");
    }

    private void rebuildToggle(Button btn) {
        btn.setMessage(toggleLabel());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // Keep toggle label in sync as ContainerData updates arrive
        // The first widget is the toggle button
        if (!renderables.isEmpty() && renderables.get(0) instanceof Button toggleBtn) {
            toggleBtn.setMessage(toggleLabel());
        }
        // Highlight the currently selected priority button
        updatePriorityHighlights();
    }

    private void updatePriorityHighlights() {
        int selected = menu.getPriority();
        // Priority buttons are widgets 1..5
        for (int i = 0; i < Priority.VALUES.length; i++) {
            int widgetIdx = 1 + i;
            if (widgetIdx < renderables.size() && renderables.get(widgetIdx) instanceof Button btn) {
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
        // Draw a plain dark panel as background
        graphics.fill(leftPos, topPos, leftPos + BG_WIDTH, topPos + BG_HEIGHT, 0xC0101010);
        // Section labels
        graphics.drawString(
                font,
                Component.translatable("screen.intellistore.filing_cabinet.open_label"),
                leftPos + (BG_WIDTH - font.width(Component.translatable("screen.intellistore.filing_cabinet.open_label"))) / 2,
                topPos + TOGGLE_Y_OFFSET - 10,
                0xFFFFFF,
                false);
        graphics.drawString(
                font,
                Component.translatable("screen.intellistore.priority_label"),
                leftPos + (BG_WIDTH - font.width(Component.translatable("screen.intellistore.priority_label"))) / 2,
                topPos + PRIORITY_Y_OFFSET - 10,
                0xFFFFFF,
                false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw title centred
        graphics.drawString(font, title, (BG_WIDTH - font.width(title)) / 2, 6, 0xFFFFFF, false);
    }
}
