package net.bobofraggins.intellistore.shared.ui;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.bobofraggins.intellistore.shared.priority.Priority;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Dialog pane that renders a priority control (▼ / ▲ buttons and current priority label).
 *
 * <p>Layout adapts to the {@code width} passed to {@link #render}, so it works correctly inside a
 * {@link ConfigDrawer} or any other width container. Button placement is always centred.
 */
public class PriorityPane implements IDialogPane {

    private static final int PANE_HEIGHT = 35;
    private static final int LABEL_Y = 3;
    private static final int ROW_Y = 14;
    private static final int BTN_W = 16;
    private static final int BTN_H = 16;
    private static final int LBL_W = 56;
    private static final int GAP = 2;
    /** Total width of [▼][gap][label][gap][▲] row. */
    private static final int ROW_W = BTN_W + GAP + LBL_W + GAP + BTN_W; // 92

    private static final ResourceLocation BTN_DOWN =
            ResourceLocation.fromNamespaceAndPath("intellistore", "textures/gui/widget/button_down.png");
    private static final ResourceLocation BTN_DOWN_FOCUSED =
            ResourceLocation.fromNamespaceAndPath("intellistore", "textures/gui/widget/button_down_focused.png");
    private static final ResourceLocation BTN_DOWN_DISABLED =
            ResourceLocation.fromNamespaceAndPath("intellistore", "textures/gui/widget/button_down_disabled.png");
    private static final ResourceLocation BTN_UP =
            ResourceLocation.fromNamespaceAndPath("intellistore", "textures/gui/widget/button_up.png");
    private static final ResourceLocation BTN_UP_FOCUSED =
            ResourceLocation.fromNamespaceAndPath("intellistore", "textures/gui/widget/button_up_focused.png");
    private static final ResourceLocation BTN_UP_DISABLED =
            ResourceLocation.fromNamespaceAndPath("intellistore", "textures/gui/widget/button_up_disabled.png");

    private final IntSupplier priorityGetter;
    private final IntConsumer prioritySetter;

    public PriorityPane(IntSupplier priorityGetter, IntConsumer prioritySetter) {
        this.priorityGetter = priorityGetter;
        this.prioritySetter = prioritySetter;
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

        boolean canDown = selected > 0;
        boolean canUp = selected < Priority.VALUES.length - 1;
        ResourceLocation downTex = !canDown
                ? BTN_DOWN_DISABLED
                : (isInButton(localMouseX, localMouseY, downBtnX) ? BTN_DOWN_FOCUSED : BTN_DOWN);
        ResourceLocation upTex =
                !canUp ? BTN_UP_DISABLED : (isInButton(localMouseX, localMouseY, upBtnX) ? BTN_UP_FOCUSED : BTN_UP);
        graphics.blit(downTex, downBtnX, ROW_Y, 0, 0, BTN_W, BTN_H, BTN_W, BTN_H);
        graphics.blit(upTex, upBtnX, ROW_Y, 0, 0, BTN_W, BTN_H, BTN_W, BTN_H);

        // Label box (no border)
        graphics.fill(lblX, ROW_Y, lblX + LBL_W, ROW_Y + BTN_H, 0xFFC6C6C6);

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
            if (selected > 0) prioritySetter.accept(selected - 1);
            return true;
        }
        if (isInButton(localX, localY, upBtnX)) {
            if (selected < Priority.VALUES.length - 1) prioritySetter.accept(selected + 1);
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
}
