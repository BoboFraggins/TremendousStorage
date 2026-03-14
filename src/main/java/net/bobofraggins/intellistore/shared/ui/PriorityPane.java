package net.bobofraggins.intellistore.shared.ui;

import java.util.function.IntSupplier;
import net.bobofraggins.intellistore.shared.network.SetPriorityPacket;
import net.bobofraggins.intellistore.shared.priority.Priority;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Dialog pane that renders a priority control (▼ / ▲ buttons and current priority label).
 *
 * <p>Layout adapts to the {@code width} passed to {@link #render}, so it works correctly inside a
 * {@link ConfigDrawer} or any other width container. Button placement is always centred.
 */
public class PriorityPane implements IDialogPane {

    private static final int PANE_HEIGHT = 33;
    private static final int LABEL_Y = 3;
    private static final int ROW_Y = 14;
    private static final int BTN_W = 20;
    private static final int BTN_H = 14;
    private static final int LBL_W = 56;
    private static final int GAP = 2;
    /** Total width of [▼][gap][label][gap][▲] row. */
    private static final int ROW_W = BTN_W + GAP + LBL_W + GAP + BTN_W; // 100

    private final IntSupplier priorityGetter;
    private final BlockPos pos;

    public PriorityPane(IntSupplier priorityGetter, BlockPos pos) {
        this.priorityGetter = priorityGetter;
        this.pos = pos;
    }

    // -------------------------------------------------------------------------
    // IDialogPane
    // -------------------------------------------------------------------------

    @Override
    public int preferredWidth() {
        return ConfigDrawer.WIDTH;
    }

    @Override
    public int preferredHeight() {
        return PANE_HEIGHT;
    }

    @Override
    public void render(
            GuiGraphics graphics, Font font, int width, int localMouseX, int localMouseY, float partialTick) {
        int selected = priorityGetter.getAsInt();
        int rowX = (width - ROW_W) / 2;
        int downBtnX = rowX;
        int upBtnX = rowX + BTN_W + GAP + LBL_W + GAP;
        int lblX = rowX + BTN_W + GAP;

        Component priorityLabel = Component.translatable("screen.intellistore.priority_label");
        graphics.drawString(font, priorityLabel, (width - font.width(priorityLabel)) / 2, LABEL_Y, 0x404040, false);

        drawButton(graphics, font, downBtnX, ROW_Y, BTN_W, BTN_H, "▼", selected > 0);
        drawButton(graphics, font, upBtnX, ROW_Y, BTN_W, BTN_H, "▲", selected < Priority.VALUES.length - 1);

        // Inset label box
        graphics.fill(lblX, ROW_Y, lblX + LBL_W, ROW_Y + 1, 0xFF373737);
        graphics.fill(lblX, ROW_Y + 1, lblX + 1, ROW_Y + BTN_H, 0xFF373737);
        graphics.fill(lblX, ROW_Y + BTN_H, lblX + LBL_W + 1, ROW_Y + BTN_H + 1, 0xFFFFFFFF);
        graphics.fill(lblX + LBL_W, ROW_Y, lblX + LBL_W + 1, ROW_Y + BTN_H, 0xFFFFFFFF);
        graphics.fill(lblX + 1, ROW_Y + 1, lblX + LBL_W, ROW_Y + BTN_H, 0xFF8B8B8B);

        Priority current = Priority.fromOrdinal(selected);
        String name = Component.translatable(current.translationKey()).getString();
        int nameX = lblX + (LBL_W - font.width(name)) / 2;
        int nameY = ROW_Y + (BTN_H - 8) / 2;
        graphics.drawString(font, name, nameX, nameY, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double localX, double localY, int button) {
        if (button != 0) return false;
        int selected = priorityGetter.getAsInt();
        int rowX = (preferredWidth() - ROW_W) / 2;
        int downBtnX = rowX;
        int upBtnX = rowX + BTN_W + GAP + LBL_W + GAP;

        if (isInButton(localX, localY, downBtnX)) {
            if (selected > 0) PacketDistributor.sendToServer(new SetPriorityPacket(pos, selected - 1));
            return true;
        }
        if (isInButton(localX, localY, upBtnX)) {
            if (selected < Priority.VALUES.length - 1)
                PacketDistributor.sendToServer(new SetPriorityPacket(pos, selected + 1));
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static boolean isInButton(double lx, double ly, int btnX) {
        return lx >= btnX && lx < btnX + BTN_W && ly >= ROW_Y && ly < ROW_Y + BTN_H;
    }

    private static void drawButton(
            GuiGraphics graphics, Font font, int x, int y, int w, int h, String label, boolean active) {
        int fillColor = active ? 0xFFC6C6C6 : 0xFF9B9B9B;
        graphics.fill(x, y, x + w, y + 1, 0xFF555555);
        graphics.fill(x, y + 1, x + 1, y + h, 0xFF555555);
        graphics.fill(x, y + h, x + w, y + h + 1, 0xFFFFFFFF);
        graphics.fill(x + w, y, x + w + 1, y + h + 1, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + w, y + h, fillColor);
        int lx = x + (w - font.width(label)) / 2;
        int ly = y + (h - 8) / 2;
        graphics.drawString(font, label, lx, ly, active ? 0x404040 : 0x707070, false);
    }
}
