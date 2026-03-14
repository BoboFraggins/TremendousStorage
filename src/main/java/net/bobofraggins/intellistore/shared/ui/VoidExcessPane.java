package net.bobofraggins.intellistore.shared.ui;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Config drawer pane for the void-excess toggle.
 *
 * <p>Renders a single toggle button whose label reflects the current on/off state, read live from
 * the supplied {@link BooleanSupplier}. Clicking fires the {@link Runnable} which should toggle
 * the setting and send the appropriate packet to the server.
 */
public class VoidExcessPane implements IDialogPane {

    private static final int PANE_HEIGHT = 26;
    private static final int BTN_H = 14;
    private static final int PAD_X = 6;
    private static final int BTN_Y = 6;

    private final BooleanSupplier stateGetter;
    private final Runnable toggleAction;

    public VoidExcessPane(BooleanSupplier stateGetter, Runnable toggleAction) {
        this.stateGetter = stateGetter;
        this.toggleAction = toggleAction;
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
        boolean on = stateGetter.getAsBoolean();
        String label = (on
                        ? Component.translatable("screen.intellistore.void_excess_on")
                        : Component.translatable("screen.intellistore.void_excess_off"))
                .getString();
        int btnW = width - 2 * PAD_X;
        drawButton(graphics, font, PAD_X, BTN_Y, btnW, BTN_H, label);
    }

    @Override
    public boolean mouseClicked(double localX, double localY, int button) {
        if (button != 0) return false;
        int btnW = preferredWidth() - 2 * PAD_X;
        if (localX >= PAD_X && localX < PAD_X + btnW && localY >= BTN_Y && localY < BTN_Y + BTN_H) {
            toggleAction.run();
            return true;
        }
        return false;
    }

    private static void drawButton(GuiGraphics graphics, Font font, int x, int y, int w, int h, String label) {
        graphics.fill(x, y, x + w, y + 1, 0xFF555555);
        graphics.fill(x, y + 1, x + 1, y + h, 0xFF555555);
        graphics.fill(x, y + h, x + w, y + h + 1, 0xFFFFFFFF);
        graphics.fill(x + w, y, x + w + 1, y + h + 1, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + w, y + h, 0xFFC6C6C6);
        int lx = x + (w - font.width(label)) / 2;
        int ly = y + (h - 8) / 2;
        graphics.drawString(font, label, lx, ly, 0x404040, false);
    }
}
