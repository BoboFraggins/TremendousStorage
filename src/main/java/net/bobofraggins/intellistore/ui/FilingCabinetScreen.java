package net.bobofraggins.intellistore.ui;

import net.bobofraggins.intellistore.network.SetPriorityPacket;
import net.bobofraggins.intellistore.network.SetVoidExcessPacket;
import net.bobofraggins.intellistore.priority.Priority;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen for the Filing Cabinet block.
 *
 * <p>Layout (176 × 212 px):
 * <ul>
 *   <li>Title centred at y=6
 *   <li>"Void Excess: ON/OFF" toggle button (120×14) centred at y=18
 *   <li>2 rows × 4 columns of folder slots starting at x=29, y=44 (18×18 each)
 *   <li>Priority label and 5 priority buttons at y=86/96
 *   <li>Player inventory 3×9 starting at y=118
 *   <li>Player hotbar at y=176
 * </ul>
 */
public class FilingCabinetScreen extends AbstractContainerScreen<FilingCabinetMenu> {

    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 204;

    private static final int BTN_W = 120;
    private static final int BTN_H = 14;
    private static final int BTN_Y = 18;

    private static final int PRIORITY_LABEL_Y = 86;
    private static final int PRIORITY_Y = 96;
    private static final int PRIORITY_BTN_W = 28;
    private static final int PRIORITY_BTN_H = 14;
    private static final int PRIORITY_BTN_GAP = 2;

    private Button voidExcessButton;

    public FilingCabinetScreen(FilingCabinetMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        // Void excess toggle
        int btnX = leftPos + (BG_WIDTH - BTN_W) / 2;
        int btnY = topPos + BTN_Y;
        voidExcessButton = addRenderableWidget(Button.builder(
                        voidExcessLabel(),
                        btn -> {
                            boolean newValue = !menu.isVoidExcess();
                            menu.setVoidExcess(newValue);
                            PacketDistributor.sendToServer(new SetVoidExcessPacket(menu.getPos(), newValue));
                        })
                .bounds(btnX, btnY, BTN_W, BTN_H)
                .build());

        // Priority buttons — 5 in a row, centred
        int totalW = Priority.VALUES.length * PRIORITY_BTN_W + (Priority.VALUES.length - 1) * PRIORITY_BTN_GAP;
        int startX = leftPos + (BG_WIDTH - totalW) / 2;
        int pY = topPos + PRIORITY_Y;

        for (int i = 0; i < Priority.VALUES.length; i++) {
            final int ordinal = i;
            Priority p = Priority.VALUES[i];
            addRenderableWidget(Button.builder(Component.translatable(p.translationKey()), btn ->
                            PacketDistributor.sendToServer(new SetPriorityPacket(menu.getPos(), ordinal)))
                    .bounds(startX + i * (PRIORITY_BTN_W + PRIORITY_BTN_GAP), pY, PRIORITY_BTN_W, PRIORITY_BTN_H)
                    .build());
        }
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Dark background
        graphics.fill(leftPos, topPos, leftPos + BG_WIDTH, topPos + BG_HEIGHT, 0xC0101010);

        // Slot backgrounds for the 8 folder slots (2 rows × 4 cols, matching menu at x=29, y=44)
        for (int i = 0; i < FilingCabinetMenu.FOLDER_SLOTS; i++) {
            int col = i % 4;
            int row = i / 4;
            int sx = leftPos + 29 + col * 18;
            int sy = topPos + 44 + row * 18;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF303030);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF1A1A1A);
        }

        // Priority label
        Component priorityLabel = Component.translatable("screen.intellistore.priority_label");
        graphics.drawString(font, priorityLabel,
                leftPos + (BG_WIDTH - font.width(priorityLabel)) / 2,
                topPos + PRIORITY_LABEL_Y, 0xAAAAAA, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (BG_WIDTH - font.width(title)) / 2, 6, 0xFFFFFF, false);
    }
}
