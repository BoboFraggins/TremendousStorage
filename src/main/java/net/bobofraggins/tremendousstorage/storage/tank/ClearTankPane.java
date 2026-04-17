package net.bobofraggins.tremendousstorage.storage.tank;

import net.bobofraggins.tremendousstorage.shared.ui.ConfigDrawer;
import net.bobofraggins.tremendousstorage.shared.ui.IDialogPane;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Config drawer pane with a "Clear Contents" button for the tank. */
public class ClearTankPane implements IDialogPane {

    private static final int PANE_HEIGHT = 28;
    private static final int BTN_Y = 7;
    private static final int BTN_H = 14;
    private static final int BTN_W = 90;
    private static final int COLOR_BTN = 0xFFC6C6C6;
    private static final int COLOR_BTN_HOVER = 0xFFB0B0B0;

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
            GuiGraphics graphics, Font font, int width, int localMouseX, int localMouseY, float partialTick) {
        int btnX = (width - BTN_W) / 2;
        boolean hovered = isInButton(localMouseX, localMouseY, btnX);
        graphics.fill(btnX, BTN_Y, btnX + BTN_W, BTN_Y + BTN_H, hovered ? COLOR_BTN_HOVER : COLOR_BTN);

        Component label = Component.translatable("screen.tremendousstorage.clear_tank_contents");
        int textX = btnX + (BTN_W - font.width(label)) / 2;
        int textY = BTN_Y + (BTN_H - font.lineHeight) / 2;
        graphics.drawString(font, label, textX, textY, 0x404040, false);
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
        return lx >= btnX && lx < btnX + BTN_W && ly >= BTN_Y && ly < BTN_Y + BTN_H;
    }
}
