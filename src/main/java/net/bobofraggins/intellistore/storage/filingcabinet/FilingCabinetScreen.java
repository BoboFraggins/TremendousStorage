package net.bobofraggins.intellistore.storage.filingcabinet;

import net.bobofraggins.intellistore.shared.network.SetPriorityPacket;
import net.bobofraggins.intellistore.shared.network.SetVoidExcessPacket;
import net.bobofraggins.intellistore.shared.priority.Priority;
import net.bobofraggins.intellistore.shared.ui.AbstractFilingCabinetScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen for the Filing Cabinet block.
 *
 * <p>Layout (176 × 204 px):
 * <ul>
 *   <li>Title centred at y=6
 *   <li>"Void Excess: ON/OFF" toggle button (120×14) centred at y=18
 *   <li>2 rows × 4 columns of folder slots starting at x=29, y=44 (18×18 each)
 *   <li>Priority label and 5 priority buttons at y=86/96
 *   <li>Player inventory 3×9 starting at y=118
 *   <li>Player hotbar at y=176
 * </ul>
 */
public class FilingCabinetScreen extends AbstractFilingCabinetScreen<FilingCabinetMenu> {

    private static final int BG_HEIGHT = 204;

    private static final int PRIORITY_LABEL_Y = 86;
    private static final int PRIORITY_Y = 96;
    private static final int PRIORITY_BTN_W = 28;
    private static final int PRIORITY_BTN_H = 14;
    private static final int PRIORITY_BTN_GAP = 2;

    public FilingCabinetScreen(FilingCabinetMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, BG_HEIGHT);
    }

    @Override
    protected Button.OnPress voidExcessAction() {
        return btn -> {
            boolean newValue = !menu.isVoidExcess();
            menu.setVoidExcess(newValue);
            PacketDistributor.sendToServer(new SetVoidExcessPacket(menu.getPos(), newValue));
        };
    }

    @Override
    protected void init() {
        super.init();

        int totalW = Priority.VALUES.length * PRIORITY_BTN_W + (Priority.VALUES.length - 1) * PRIORITY_BTN_GAP;
        int startX = leftPos + (BG_WIDTH - totalW) / 2;
        int pY = topPos + PRIORITY_Y;

        for (int i = 0; i < Priority.VALUES.length; i++) {
            final int ordinal = i;
            Priority p = Priority.VALUES[i];
            addRenderableWidget(Button.builder(
                            Component.translatable(p.translationKey()),
                            btn -> PacketDistributor.sendToServer(new SetPriorityPacket(menu.getPos(), ordinal)))
                    .bounds(startX + i * (PRIORITY_BTN_W + PRIORITY_BTN_GAP), pY, PRIORITY_BTN_W, PRIORITY_BTN_H)
                    .build());
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updatePriorityHighlights();
    }

    private void updatePriorityHighlights() {
        int selected = menu.getPriority();
        // Priority buttons are widgets 1..5 (voidExcess is widget 0)
        for (int i = 0; i < Priority.VALUES.length; i++) {
            int widgetIdx = 1 + i;
            if (widgetIdx < renderables.size() && renderables.get(widgetIdx) instanceof Button btn) {
                btn.active = (i != selected);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);

        Component priorityLabel = Component.translatable("screen.intellistore.priority_label");
        graphics.drawString(
                font,
                priorityLabel,
                leftPos + (BG_WIDTH - font.width(priorityLabel)) / 2,
                topPos + PRIORITY_LABEL_Y,
                0xAAAAAA,
                false);
    }
}
