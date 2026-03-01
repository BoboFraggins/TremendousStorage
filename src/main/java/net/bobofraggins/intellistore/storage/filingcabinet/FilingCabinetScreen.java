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
 *   <li>Title centred at y=4
 *   <li>"Void Excess: ON/OFF" toggle button (120×14) centred at y=18
 *   <li>2 rows × 4 columns of folder slots starting at x=29, y=44 (18×18 each)
 *   <li>"Priority:" label centred at y=86
 *   <li>[▼] [name] [▲] row centred at y=96
 *   <li>Player inventory 3×9 starting at y=118
 *   <li>Player hotbar at y=176
 * </ul>
 */
public class FilingCabinetScreen extends AbstractFilingCabinetScreen<FilingCabinetMenu> {

    private static final int BG_HEIGHT = 204;

    private static final int PRIORITY_LABEL_Y = 86;
    private static final int PRIORITY_ROW_Y   = 96;
    private static final int BTN_W  = 20;
    private static final int BTN_H  = 14;
    private static final int LBL_W  = 56;
    private static final int GAP    = 2;
    private static final int ROW_W  = BTN_W + GAP + LBL_W + GAP + BTN_W;

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

        int rowX = leftPos + (BG_WIDTH - ROW_W) / 2;
        int btnY = topPos + PRIORITY_ROW_Y;

        // ▼ — decrease priority
        addRenderableWidget(Button.builder(Component.literal("▼"), btn -> {
                    int next = Math.max(0, menu.getPriority() - 1);
                    PacketDistributor.sendToServer(new SetPriorityPacket(menu.getPos(), next));
                })
                .bounds(rowX, btnY, BTN_W, BTN_H)
                .build());

        // ▲ — increase priority
        addRenderableWidget(Button.builder(Component.literal("▲"), btn -> {
                    int next = Math.min(Priority.VALUES.length - 1, menu.getPriority() + 1);
                    PacketDistributor.sendToServer(new SetPriorityPacket(menu.getPos(), next));
                })
                .bounds(rowX + BTN_W + GAP + LBL_W + GAP, btnY, BTN_W, BTN_H)
                .build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int selected = menu.getPriority();
        // Widgets: 0=voidExcess (from super), 1=▼, 2=▲
        if (renderables.size() >= 3) {
            if (renderables.get(1) instanceof Button down) down.active = (selected > 0);
            if (renderables.get(2) instanceof Button up)   up.active   = (selected < Priority.VALUES.length - 1);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);

        // "Priority:" label
        Component priorityLabel = Component.translatable("screen.intellistore.priority_label");
        graphics.drawString(font, priorityLabel,
                leftPos + (BG_WIDTH - font.width(priorityLabel)) / 2,
                topPos + PRIORITY_LABEL_Y, 0x404040, false);

        // Priority name display — inset box between the two arrow buttons
        int rowX  = leftPos + (BG_WIDTH - ROW_W) / 2;
        int lblX0 = rowX + BTN_W + GAP;
        int lblY  = topPos + PRIORITY_ROW_Y;

        graphics.fill(lblX0,         lblY,         lblX0 + LBL_W,     lblY + 1,         0xFF373737);
        graphics.fill(lblX0,         lblY + 1,     lblX0 + 1,         lblY + BTN_H,     0xFF373737);
        graphics.fill(lblX0,         lblY + BTN_H, lblX0 + LBL_W + 1, lblY + BTN_H + 1, 0xFFFFFFFF);
        graphics.fill(lblX0 + LBL_W, lblY,         lblX0 + LBL_W + 1, lblY + BTN_H,     0xFFFFFFFF);
        graphics.fill(lblX0 + 1,     lblY + 1,     lblX0 + LBL_W,     lblY + BTN_H,     0xFF8B8B8B);

        Priority current = Priority.fromOrdinal(menu.getPriority());
        String name = Component.translatable(current.translationKey()).getString();
        int nameX = lblX0 + (LBL_W - font.width(name)) / 2;
        int nameY = lblY + (BTN_H - 8) / 2;
        graphics.drawString(font, name, nameX, nameY, 0x404040, false);
    }
}
