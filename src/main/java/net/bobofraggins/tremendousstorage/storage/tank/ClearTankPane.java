package net.bobofraggins.tremendousstorage.storage.tank;

import net.bobofraggins.tremendousstorage.shared.ui.ConfigDrawer;
import net.bobofraggins.tremendousstorage.shared.ui.IDialogPane;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Config drawer pane with a "Clear Contents" button for the tank. */
public class ClearTankPane implements IDialogPane {

    private static final int PANE_HEIGHT = 29;
    private static final int BTN_Y = 7;
    private static final int BTN_H = 14;
    private static final int BTN_W = 90;
    private static final int COLOR_FILL = 0xFFC6C6C6;
    private static final int COLOR_FILL_HOVER = 0xFFD4D4D4;
    private static final int COLOR_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_DARK = 0xFF555555;

    private final Runnable clearAction;

    public ClearTankPane(Runnable clearAction) {
        this.clearAction = clearAction;
    }

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
            GuiGraphicsExtractor graphics, Font font, int width, int localMouseX, int localMouseY, float partialTick) {
        int btnX = (width - BTN_W) / 2;
        boolean hovered = isInButton(localMouseX, localMouseY, btnX);

        // Raised bevel button: light top/left, dark bottom/right
        graphics.fill(btnX, BTN_Y, btnX + BTN_W, BTN_Y + 1, COLOR_LIGHT); // top
        graphics.fill(btnX, BTN_Y + 1, btnX + 1, BTN_Y + BTN_H, COLOR_LIGHT); // left
        graphics.fill(btnX, BTN_Y + BTN_H, btnX + BTN_W, BTN_Y + BTN_H + 1, COLOR_DARK); // bottom
        graphics.fill(btnX + BTN_W, BTN_Y, btnX + BTN_W + 1, BTN_Y + BTN_H, COLOR_DARK); // right
        graphics.fill(
                btnX + 1, BTN_Y + 1, btnX + BTN_W, BTN_Y + BTN_H, hovered ? COLOR_FILL_HOVER : COLOR_FILL); // fill

        Component label = Component.translatable("screen.tremendousstorage.clear_tank_contents");
        int textX = btnX + (BTN_W - font.width(label)) / 2;
        int textY = BTN_Y + (BTN_H - font.lineHeight) / 2;
        graphics.text(font, label, textX, textY, 0xFF404040, false);
    }

    @Override
    public boolean mouseClicked(double localX, double localY, int button) {
        if (button != 0) return false;
        int btnX = (preferredWidth() - BTN_W) / 2;
        if (isInButton(localX, localY, btnX)) {
            clearAction.run();
            return true;
        }
        return false;
    }

    private static boolean isInButton(double lx, double ly, int btnX) {
        return lx >= btnX && lx < btnX + BTN_W + 1 && ly >= BTN_Y && ly < BTN_Y + BTN_H + 1;
    }
}
